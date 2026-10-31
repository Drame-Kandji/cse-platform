package sn.cse.reporting.domain.model;

/** État courant d'un matériel (contrat §1). */
public enum EtatMateriel {
  /** Disponible. */
  DISPONIBLE("Disponible"),
  /** Affecté. */
  AFFECTE("Affecté"),
  /** En maintenance. */
  EN_MAINTENANCE("En maintenance"),
  /** Réformé. */
  REFORME("Réformé");

  private final String libelle;

  EtatMateriel(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
