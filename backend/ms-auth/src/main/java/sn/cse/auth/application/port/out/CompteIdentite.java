package sn.cse.auth.application.port.out;

import java.util.Set;
import java.util.UUID;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Utilisateur.Identite;

/**
 * Compte lu dans le fournisseur d'identité (synchronisation).
 *
 * @param id identifiant ({@code sub})
 * @param identite identité (sans téléphone : donnée propre à ms-auth)
 * @param actif compte activé
 * @param roles codes des rôles realm du compte
 * @param attributs attributs spécifiques
 * @param otpConfigure second facteur TOTP enregistré
 */
public record CompteIdentite(
    UUID id,
    Identite identite,
    boolean actif,
    Set<String> roles,
    AttributsProfil attributs,
    boolean otpConfigure) {

  public CompteIdentite {
    roles = roles == null ? Set.of() : Set.copyOf(roles);
  }
}
