package sn.cse.auth.application.command;

import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur.Identite;

/**
 * Création d'un compte par un administrateur.
 *
 * @param identite identité
 * @param role rôle unique du compte
 * @param attributs attributs spécifiques
 * @param motDePasseTemporaire mot de passe temporaire transmis à Keycloak
 */
public record CreerUtilisateurCommand(
    Identite identite, Role role, AttributsProfil attributs, String motDePasseTemporaire) {

  @Override
  public String toString() {
    return "CreerUtilisateurCommand[email=" + identite.email() + ", role=" + role + "]";
  }
}
