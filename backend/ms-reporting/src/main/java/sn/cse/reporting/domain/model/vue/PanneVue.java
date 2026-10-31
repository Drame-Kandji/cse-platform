package sn.cse.reporting.domain.model.vue;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import sn.cse.reporting.domain.model.GravitePanne;

/**
 * Vue d'une panne signalée. L'événement {@code PANNE_SIGNALEE} ne porte pas de statut : une panne
 * est considérée résolue dès qu'une maintenance du même matériel est clôturée après son
 * signalement.
 *
 * @param id identifiant
 * @param materielId matériel
 * @param chantierId chantier du signalement
 * @param gravite gravité
 * @param signaleeLe instant du signalement
 * @param resolueLe instant de la clôture de maintenance qui la résout
 */
public record PanneVue(
    UUID id,
    UUID materielId,
    UUID chantierId,
    GravitePanne gravite,
    Instant signaleeLe,
    Instant resolueLe) {

  public PanneVue {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(signaleeLe, "signaleeLe");
  }

  /**
   * @param clotureMaintenance instant de clôture d'une maintenance du même matériel
   * @return la panne résolue si la clôture est postérieure au signalement
   */
  public PanneVue resolueSi(Instant clotureMaintenance) {
    if (resolueLe != null
        || clotureMaintenance == null
        || clotureMaintenance.isBefore(signaleeLe)) {
      return this;
    }
    return new PanneVue(id, materielId, chantierId, gravite, signaleeLe, clotureMaintenance);
  }

  /**
   * @return vrai tant que la panne n'est pas résolue
   */
  public boolean ouverte() {
    return resolueLe == null;
  }
}
