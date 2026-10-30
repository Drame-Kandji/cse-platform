package sn.cse.auth.domain.model;

/** Administrateur système : comptes, rôles, référentiels et audit. Aucun attribut spécifique. */
public final class AdministrateurSysteme extends Utilisateur {

  /**
   * @param etat état commun
   */
  public AdministrateurSysteme(EtatCommun etat) {
    super(etat);
  }

  @Override
  public Role role() {
    return Role.ADMIN_SYSTEME;
  }

  @Override
  public AttributsProfil attributs() {
    return AttributsProfil.AUCUN;
  }

  @Override
  public void modifierAttributs(AttributsProfil attributs) {
    // Profil sans attribut spécifique : les attributs des autres profils sont ignorés (Liskov).
  }
}
