package sn.cse.reporting.domain.model;

/** Gravité d'un litige fournisseur (contrat §1). */
public enum GraviteLitige {
  /** Faible. */
  FAIBLE("Faible"),
  /** Moyenne. */
  MOYENNE("Moyenne"),
  /** Élevée. */
  ELEVEE("Élevée");

  private final String libelle;

  GraviteLitige(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
