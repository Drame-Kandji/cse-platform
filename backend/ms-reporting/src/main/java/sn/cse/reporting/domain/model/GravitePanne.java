package sn.cse.reporting.domain.model;

/** Gravité d'une panne (contrat §1). */
public enum GravitePanne {
  /** Mineure. */
  MINEURE("Mineure"),
  /** Majeure. */
  MAJEURE("Majeure"),
  /** Bloquante. */
  BLOQUANTE("Bloquante");

  private final String libelle;

  GravitePanne(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
