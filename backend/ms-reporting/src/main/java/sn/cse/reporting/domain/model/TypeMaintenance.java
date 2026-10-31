package sn.cse.reporting.domain.model;

/** Type d'intervention de maintenance (contrat §1). */
public enum TypeMaintenance {
  /** Préventive. */
  PREVENTIVE("Préventive"),
  /** Corrective. */
  CORRECTIVE("Corrective"),
  /** Lourde. */
  LOURDE("Lourde");

  private final String libelle;

  TypeMaintenance(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
