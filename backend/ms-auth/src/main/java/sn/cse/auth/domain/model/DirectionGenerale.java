package sn.cse.auth.domain.model;

/**
 * Membre de la Direction Générale : pilotage et approbations stratégiques. Aucun attribut
 * spécifique.
 */
public final class DirectionGenerale extends Utilisateur {

  /**
   * @param etat état commun
   */
  public DirectionGenerale(EtatCommun etat) {
    super(etat);
  }

  @Override
  public Role role() {
    return Role.DIRECTION_GENERALE;
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
