package sn.cse.reporting.domain.model;

/** Statut d'un contrat d'achat (contrat §1). */
public enum StatutContrat {
  /** Brouillon. */
  BROUILLON("Brouillon"),
  /** Signé par le CSE, en attente de la signature du fournisseur. */
  EN_ATTENTE_SIGNATURE("En attente de signature"),
  /** Signé. */
  SIGNE("Signé"),
  /** Engagement validé. */
  ENGAGEMENT_VALIDE("Engagement validé"),
  /** En livraison. */
  EN_LIVRAISON("En livraison"),
  /** Livré. */
  LIVRE("Livré"),
  /** Clôturé. */
  CLOTURE("Clôturé");

  private final String libelle;

  StatutContrat(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
