package sn.cse.auth.application.port.out;

import java.time.Duration;
import java.util.Objects;

/**
 * Paramètres du lien d'activation envoyé par e-mail par le fournisseur d'identité.
 *
 * @param clientId client OpenID Connect vers lequel l'utilisateur est renvoyé (ex. {@code
 *     cse-portail-fournisseur})
 * @param redirectUri adresse de retour après la définition du mot de passe (autorisée par le
 *     client)
 * @param validite durée de validité du lien
 */
public record LienActivation(String clientId, String redirectUri, Duration validite) {

  public LienActivation {
    Objects.requireNonNull(clientId, "clientId");
    Objects.requireNonNull(redirectUri, "redirectUri");
    Objects.requireNonNull(validite, "validite");
    if (validite.isNegative() || validite.isZero()) {
      throw new IllegalArgumentException("La durée de validité du lien doit être positive");
    }
  }
}
