package sn.cse.reporting.domain.model;

/** Sections d'un tableau de bord (contrat §6). */
public enum Section {
  /** Parc matériel. */
  PARC("Parc matériel"),
  /** Besoins matériels. */
  BESOINS("Besoins"),
  /** Appels d'offres, contrats et livraisons. */
  ACHATS("Achats"),
  /** Performance des fournisseurs. */
  FOURNISSEURS("Fournisseurs"),
  /** Budget, engagements, mandatements et coûts. */
  FINANCES("Finances"),
  /** Indicateurs par chantier. */
  CHANTIERS("Chantiers");

  private final String libelle;

  Section(String libelle) {
    this.libelle = libelle;
  }

  /**
   * @return libellé français
   */
  public String libelle() {
    return libelle;
  }
}
