package sn.cse.auth.application.command;

import java.util.UUID;

/**
 * Réinitialisation du mot de passe d'un compte.
 *
 * @param id compte visé
 * @param motDePasseTemporaire mot de passe temporaire
 */
public record ReinitialiserMotDePasseCommand(UUID id, String motDePasseTemporaire) {

  @Override
  public String toString() {
    return "ReinitialiserMotDePasseCommand[id=" + id + "]";
  }
}
