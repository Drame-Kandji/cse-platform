package sn.cse.reporting.domain.model;

/** Priorité d'un besoin (contrat §1). */
public enum Priorite {
  /** Normal. */
  NORMAL("Normal"),
  /** Urgent. */
  URGENT("Urgent"),
  /** Critique. */
  CRITIQUE("Critique");

  private final String libelle;

  Priorite(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
