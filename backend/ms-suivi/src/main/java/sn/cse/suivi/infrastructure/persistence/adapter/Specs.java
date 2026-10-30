package sn.cse.suivi.infrastructure.persistence.adapter;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.data.jpa.domain.Specification;
import sn.cse.common.pagination.PageResponse;
import sn.cse.suivi.application.query.Perimetre;

/** Briques de critères JPA réutilisables (égalité optionnelle, texte libre, périmètre). */
final class Specs {

  private Specs() {}

  /**
   * @param attribut attribut de l'entité
   * @param valeur valeur (critère ignoré si {@code null})
   * @param <T> entité
   * @return le critère
   */
  static <T> Specification<T> egal(String attribut, Object valeur) {
    return (root, q, cb) -> valeur == null ? null : cb.equal(root.get(attribut), valeur);
  }

  /**
   * @param texte texte libre (ignoré si vide)
   * @param attributs attributs textuels comparés sans casse
   * @param <T> entité
   * @return le critère « contient » sur l'un des attributs
   */
  static <T> Specification<T> contient(String texte, String... attributs) {
    return (root, q, cb) -> {
      if (texte == null || texte.isBlank()) {
        return null;
      }
      String motif = "%" + texte.trim().toLowerCase(Locale.ROOT) + "%";
      return cb.or(
          Stream.of(attributs)
              .map(a -> cb.like(cb.lower(root.get(a)), motif))
              .toArray(jakarta.persistence.criteria.Predicate[]::new));
    };
  }

  /**
   * @param attribut attribut portant l'identifiant de chantier
   * @param perimetre chantiers visibles
   * @param <T> entité
   * @return le critère de périmètre (aucun résultat si le périmètre est vide)
   */
  static <T> Specification<T> perimetre(String attribut, Perimetre perimetre) {
    return (root, q, cb) -> {
      if (perimetre == null || !perimetre.restreint()) {
        return null;
      }
      return perimetre.chantierIds().isEmpty()
          ? cb.disjunction()
          : root.get(attribut).in(perimetre.chantierIds());
    };
  }

  /**
   * @param criteres critères à combiner (les {@code null} sont ignorés)
   * @param <T> entité
   * @return la conjonction
   */
  @SafeVarargs
  static <T> Specification<T> tous(Specification<T>... criteres) {
    return Stream.of(criteres)
        .filter(Objects::nonNull)
        .reduce(Specification.where(null), Specification::and);
  }

  /**
   * @param page page Spring Data
   * @param mapper conversion
   * @param <E> entité
   * @param <D> domaine
   * @return la page de l'API
   */
  static <E, D> PageResponse<D> page(
      org.springframework.data.domain.Page<E> page, java.util.function.Function<E, D> mapper) {
    return PageResponse.of(
        page.getContent().stream().map(mapper).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements());
  }
}
