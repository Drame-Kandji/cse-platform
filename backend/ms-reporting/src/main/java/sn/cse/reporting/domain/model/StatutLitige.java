package sn.cse.reporting.domain.model;

/** Statut d'un litige fournisseur (contrat §1). */
public enum StatutLitige {
  /** Ouvert. */
  OUVERT("Ouvert"),
  /** En cours. */
  EN_COURS("En cours"),
  /** Résolu. */
  RESOLU("Résolu"),
  /** Classé. */
  CLASSE("Classé");

  private final String libelle;

  StatutLitige(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
