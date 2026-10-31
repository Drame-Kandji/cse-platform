package sn.cse.reporting.domain.model;

import java.util.Set;
import java.util.UUID;

/**
 * Périmètre de chantiers d'un tableau de bord : tous les chantiers, ou un ensemble restreint (chef
 * de chantier, ou filtre {@code chantierId}).
 *
 * @param restreint vrai si seuls {@link #chantierIds()} sont visibles
 * @param chantierIds chantiers visibles quand le périmètre est restreint
 */
public record Perimetre(boolean restreint, Set<UUID> chantierIds) {

  public Perimetre {
    chantierIds = chantierIds == null ? Set.of() : Set.copyOf(chantierIds);
  }

  /**
   * @return périmètre sans restriction
   */
  public static Perimetre tous() {
    return new Perimetre(false, Set.of());
  }

  /**
   * @param chantierIds chantiers visibles
   * @return périmètre restreint
   */
  public static Perimetre chantiers(Set<UUID> chantierIds) {
    return new Perimetre(true, chantierIds);
  }

  /**
   * @param chantierId chantier d'une donnée (peut être {@code null} : donnée hors chantier)
   * @return vrai si la donnée est visible dans ce périmètre
   */
  public boolean inclut(UUID chantierId) {
    return !restreint || (chantierId != null && chantierIds.contains(chantierId));
  }
}
