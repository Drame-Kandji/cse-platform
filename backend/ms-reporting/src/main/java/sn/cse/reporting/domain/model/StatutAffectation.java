package sn.cse.reporting.domain.model;

/** Statut d'une affectation (contrat §1). */
public enum StatutAffectation {
  /** En attente de réception. */
  EN_ATTENTE_RECEPTION("En attente de réception"),
  /** En cours. */
  EN_COURS("En cours"),
  /** Terminée. */
  TERMINEE("Terminée");

  private final String libelle;

  StatutAffectation(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
