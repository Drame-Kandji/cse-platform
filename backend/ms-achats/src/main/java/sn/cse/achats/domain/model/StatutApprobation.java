package sn.cse.achats.domain.model;

/** Statut d'une approbation (co-validation, approbation DG, engagement DAF). */
public enum StatutApprobation {
  /** Décision attendue. */
  EN_ATTENTE,
  /** Approuvée. */
  APPROUVEE,
  /** Refusée. */
  REFUSEE
}
