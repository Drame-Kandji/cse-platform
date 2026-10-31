package sn.cse.achats.domain.model;

/** Issue d'une décision sur une proposition d'attribution (R11). */
public enum ResultatDecision {
  /** Décision enregistrée, d'autres approbations sont encore attendues. */
  EN_ATTENTE_AUTRES_APPROBATIONS,
  /** Toutes les approbations sont acquises : le marché est attribué. */
  MARCHE_ATTRIBUE,
  /** Refus : la proposition d'attribution est supprimée. */
  ATTRIBUTION_REFUSEE
}
