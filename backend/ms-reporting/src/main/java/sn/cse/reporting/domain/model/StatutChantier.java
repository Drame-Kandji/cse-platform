package sn.cse.reporting.domain.model;

/** Statut d'un chantier (contrat §1). */
public enum StatutChantier {
  /** En cours. */
  EN_COURS("En cours"),
  /** Terminé. */
  TERMINE("Terminé");

  private final String libelle;

  StatutChantier(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
