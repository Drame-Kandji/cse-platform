package sn.cse.achats.domain.model;

/** Statut d'un jalon de contrat, calculé à une date donnée. */
public enum StatutJalon {
  /** Échéance non atteinte. */
  A_VENIR,
  /** Jalon réalisé. */
  ATTEINT,
  /** Échéance dépassée sans réalisation. */
  EN_RETARD
}
