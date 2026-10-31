package sn.cse.reporting.domain.model;

/** Statut d'une facture fournisseur (contrat §1). */
public enum StatutFacture {
  /** Reçue. */
  RECUE("Reçue"),
  /** Mandatée. */
  MANDATEE("Mandatée"),
  /** Payée. */
  PAYEE("Payée"),
  /** Rejetée. */
  REJETEE("Rejetée");

  private final String libelle;

  StatutFacture(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
