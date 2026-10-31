package sn.cse.reporting.domain.model;

/** Statut d'un appel d'offres (contrat §1). */
public enum StatutAppelOffre {
  /** Publié. */
  PUBLIE("Publié"),
  /** Clôturé. */
  CLOTURE("Clôturé"),
  /** Attribué. */
  ATTRIBUE("Attribué");

  private final String libelle;

  StatutAppelOffre(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
