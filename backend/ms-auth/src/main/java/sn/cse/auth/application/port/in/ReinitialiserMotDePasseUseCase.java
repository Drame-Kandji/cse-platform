package sn.cse.auth.application.port.in;

import sn.cse.auth.application.command.ReinitialiserMotDePasseCommand;

/** Réinitialisation du mot de passe d'un compte (mot de passe temporaire Keycloak). */
public interface ReinitialiserMotDePasseUseCase {

  /**
   * @param commande compte et mot de passe temporaire
   */
  void reinitialiser(ReinitialiserMotDePasseCommand commande);
}
