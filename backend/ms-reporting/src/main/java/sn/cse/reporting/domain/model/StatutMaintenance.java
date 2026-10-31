package sn.cse.reporting.domain.model;

/** Statut d'une maintenance (contrat §1). */
public enum StatutMaintenance {
  /** Planifiée. */
  PLANIFIEE("Planifiée"),
  /** En cours. */
  EN_COURS("En cours"),
  /** Clôturée. */
  CLOTUREE("Clôturée");

  private final String libelle;

  StatutMaintenance(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
