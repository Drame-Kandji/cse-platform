package sn.cse.suivi.application.fake;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.suivi.application.query.Perimetre;

/**
 * Stockage en mémoire d'instantanés immuables. Comme la base : chaque lecture renvoie une instance
 * neuve, chaque enregistrement incrémente la version et refuse une version obsolète.
 *
 * @param <A> agrégat
 * @param <S> instantané
 */
final class Stock<A, S> {

  private final Map<UUID, S> lignes = new LinkedHashMap<>();
  private final Function<A, S> instantane;
  private final Function<S, A> reconstituer;
  private final Function<S, UUID> id;
  private final Function<S, Long> version;
  private final BiFunction<S, Long, S> versionner;

  Stock(
      Function<A, S> instantane,
      Function<S, A> reconstituer,
      Function<S, UUID> id,
      Function<S, Long> version,
      BiFunction<S, Long, S> versionner) {
    this.instantane = instantane;
    this.reconstituer = reconstituer;
    this.id = id;
    this.version = version;
    this.versionner = versionner;
  }

  A save(A agregat) {
    S s = instantane.apply(agregat);
    S existant = lignes.get(id.apply(s));
    Long attendue = existant == null ? null : version.apply(existant);
    if (!Objects.equals(attendue, version.apply(s))) {
      throw new IllegalStateException("Version obsolète pour " + id.apply(s));
    }
    Long suivante = version.apply(s) == null ? 0L : version.apply(s) + 1;
    S enregistre = versionner.apply(s, suivante);
    lignes.put(id.apply(enregistre), enregistre);
    return reconstituer.apply(enregistre);
  }

  Optional<A> findById(UUID cle) {
    return Optional.ofNullable(lignes.get(cle)).map(reconstituer);
  }

  List<A> filtrer(Predicate<A> critere) {
    return lignes.values().stream().map(reconstituer).filter(critere).toList();
  }

  List<A> tous() {
    return filtrer(a -> true);
  }

  static <T> PageResponse<T> page(List<T> tous, PageQuery page) {
    int debut = Math.min(page.page() * page.size(), tous.size());
    int fin = Math.min(debut + page.size(), tous.size());
    return PageResponse.of(tous.subList(debut, fin), page.page(), page.size(), tous.size());
  }

  static boolean visible(Perimetre perimetre, UUID chantierId) {
    return perimetre == null
        || !perimetre.restreint()
        || (chantierId != null && perimetre.chantierIds().contains(chantierId));
  }
}
