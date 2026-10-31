package sn.cse.reporting.domain.model;

/** Statut d'un fournisseur (contrat §1). */
public enum StatutFournisseur {
  /** En attente de validation. */
  EN_ATTENTE_VALIDATION("En attente de validation"),
  /** Actif. */
  ACTIF("Actif"),
  /** Suspendu. */
  SUSPENDU("Suspendu"),
  /** Blacklisté. */
  BLACKLISTE("Blacklisté");

  private final String libelle;

  StatutFournisseur(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
