package sn.cse.auth.application.port.out;

import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur.Identite;

/**
 * Données de création d'un compte dans le fournisseur d'identité.
 *
 * @param identite identité (l'e-mail sert d'identifiant de connexion)
 * @param role rôle prévu (attribué ensuite par {@link IdentityProviderPort#assignerRole})
 * @param attributs attributs spécifiques
 * @param motDePasseTemporaire mot de passe temporaire (jamais conservé par ms-auth) ; {@code null}
 *     pour un compte sans mot de passe, activé par l'e-mail « définir votre mot de passe »
 * @param otpObligatoire vrai si la configuration TOTP doit être exigée
 */
public record NouveauCompte(
    Identite identite,
    Role role,
    AttributsProfil attributs,
    String motDePasseTemporaire,
    boolean otpObligatoire) {

  // Le mot de passe ne doit jamais apparaître dans les journaux.
  @Override
  public String toString() {
    return "NouveauCompte[email=" + identite.email() + ", role=" + role + "]";
  }
}
