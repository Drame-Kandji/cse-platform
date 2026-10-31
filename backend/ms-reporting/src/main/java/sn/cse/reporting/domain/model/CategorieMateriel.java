package sn.cse.reporting.domain.model;

/** Catégorie de matériel (contrat §1). */
public enum CategorieMateriel {
  /** Matériel roulant. */
  MATERIEL_ROULANT("Matériel roulant"),
  /** Outillage. */
  OUTILLAGE("Outillage"),
  /** Équipement informatique. */
  EQUIPEMENT_INFORMATIQUE("Équipement informatique"),
  /** Consommable. */
  CONSOMMABLE("Consommable");

  private final String libelle;

  CategorieMateriel(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
