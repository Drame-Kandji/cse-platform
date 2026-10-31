package sn.cse.reporting.domain.model;

/** Statut d'un besoin matériel (contrat §1). */
public enum StatutBesoin {
  /** En attente. */
  EN_ATTENTE("En attente"),
  /** Validé. */
  VALIDE("Validé"),
  /** Rejeté. */
  REJETE("Rejeté"),
  /** Affecté directement. */
  AFFECTE_DIRECT("Affecté directement"),
  /** En achat. */
  EN_ACHAT("En achat");

  private final String libelle;

  StatutBesoin(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
