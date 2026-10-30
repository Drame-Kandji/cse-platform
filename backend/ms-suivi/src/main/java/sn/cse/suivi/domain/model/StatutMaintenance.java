package sn.cse.suivi.domain.model;

import sn.cse.suivi.domain.exception.SuiviErreurs;

/** Cycle de vie d'un ordre de travail : {@code PLANIFIEE → EN_COURS → CLOTUREE}. */
public enum StatutMaintenance {
  /** Intervention planifiée. */
  PLANIFIEE,
  /** Intervention en cours : le matériel est immobilisé. */
  EN_COURS,
  /** Intervention terminée. */
  CLOTUREE;

  /**
   * @return le statut suivant, ou {@code null} pour l'état terminal
   */
  public StatutMaintenance suivant() {
    return switch (this) {
      case PLANIFIEE -> EN_COURS;
      case EN_COURS -> CLOTUREE;
      case CLOTUREE -> null;
    };
  }

  /**
   * @param cible statut visé
   * @throws sn.cse.common.exception.ConflictException si {@code cible} n'est pas le statut suivant
   */
  public void verifierTransition(StatutMaintenance cible) {
    if (suivant() != cible) {
      throw SuiviErreurs.transitionInvalide("la maintenance", this, cible);
    }
  }
}
