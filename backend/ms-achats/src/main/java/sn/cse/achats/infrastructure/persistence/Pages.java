package sn.cse.achats.infrastructure.persistence;

import java.util.Map;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Conversions de pagination entre le contrat applicatif et Spring Data. */
final class Pages {

  private Pages() {}

  /**
   * @param q demande validée (tri en liste blanche)
   * @param proprietes correspondance champ exposé → propriété JPA (identité par défaut)
   * @return la pagination Spring Data
   */
  static Pageable pageable(PageQuery q, Map<String, String> proprietes) {
    if (q.sortField() == null) {
      return PageRequest.of(q.page(), q.size());
    }
    String propriete = proprietes.getOrDefault(q.sortField(), q.sortField());
    Sort.Direction sens = q.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC;
    // Le tri secondaire sur l'identifiant garantit un ordre stable entre les pages.
    return PageRequest.of(q.page(), q.size(), Sort.by(sens, propriete).and(Sort.by("id")));
  }

  static Pageable pageable(PageQuery q) {
    return pageable(q, Map.of());
  }

  static <E, D> PageResponse<D> reponse(Page<E> page, PageQuery q, Function<E, D> mapper) {
    return PageResponse.of(
        page.getContent().stream().map(mapper).toList(),
        q.page(),
        q.size(),
        page.getTotalElements());
  }
}
