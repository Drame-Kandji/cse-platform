package sn.cse.suivi.domain.model;

import sn.cse.suivi.domain.exception.SuiviErreurs;

/** Cycle de vie d'une panne : {@code SIGNALEE → PRISE_EN_CHARGE → RESOLUE}. */
public enum StatutPanne {
  /** Signalée par le chantier. */
  SIGNALEE,
  /** Rattachée à un ordre de travail. */
  PRISE_EN_CHARGE,
  /** Réparée (ordre de travail clôturé). */
  RESOLUE;

  /**
   * @param cible statut visé
   * @throws sn.cse.common.exception.ConflictException si la transition est interdite
   */
  public void verifierTransition(StatutPanne cible) {
    boolean ok =
        (this == SIGNALEE && cible == PRISE_EN_CHARGE)
            || (this == PRISE_EN_CHARGE && cible == RESOLUE);
    if (!ok) {
      throw SuiviErreurs.transitionInvalide("la panne", this, cible);
    }
  }
}
