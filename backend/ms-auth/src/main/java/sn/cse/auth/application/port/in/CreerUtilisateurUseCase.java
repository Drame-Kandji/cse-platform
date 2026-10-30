package sn.cse.auth.application.port.in;

import sn.cse.auth.application.command.CreerUtilisateurCommand;
import sn.cse.auth.domain.model.Utilisateur;

/** Création d'un compte (Keycloak + profil) par l'administrateur. */
public interface CreerUtilisateurUseCase {

  /**
   * @param commande données du compte
   * @return le profil créé
   */
  Utilisateur creer(CreerUtilisateurCommand commande);
}
