package sn.cse.e2e.support;

/**
 * Comptes de démonstration (contrat d'API §12), un par rôle métier, plus le fournisseur F1 (Sahel
 * Engins SA, Moussa Kane) du portail fournisseur.
 */
public enum Compte {
  CHEF("chef.chantier@cse.sn"),
  RM("resp.materiel@cse.sn"),
  RA("resp.achats@cse.sn"),
  DAF("daf@cse.sn"),
  DG("dg@cse.sn"),
  ADMIN("admin@cse.sn"),
  FOURNISSEUR("fournisseur@cse.sn");

  private final String email;

  Compte(String email) {
    this.email = email;
  }

  /**
   * Identifiant de connexion Keycloak.
   *
   * @return e-mail du compte
   */
  public String email() {
    return email;
  }
}
