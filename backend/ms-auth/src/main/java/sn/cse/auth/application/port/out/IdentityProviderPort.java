package sn.cse.auth.application.port.out;

import java.util.List;
import java.util.UUID;
import sn.cse.auth.domain.model.Role;

/**
 * Port vers le fournisseur d'identité (Keycloak) : seul détenteur des mots de passe et des seconds
 * facteurs (ADR-0012). Toute indisponibilité est signalée par {@link
 * sn.cse.auth.application.exception.IdentiteIndisponibleException}.
 */
public interface IdentityProviderPort {

  /**
   * Crée le compte avec un mot de passe temporaire et les actions requises (changement de mot de
   * passe, configuration TOTP si obligatoire). Le rôle n'est pas encore attribué.
   *
   * @param compte données du compte
   * @return identifiant ({@code sub}) attribué par le fournisseur d'identité
   */
  UUID creerCompte(NouveauCompte compte);

  /**
   * Supprime un compte (compensation d'une création avortée).
   *
   * @param id identifiant du compte
   */
  void supprimerCompte(UUID id);

  /**
   * Met à jour nom, prénom, e-mail et attributs (chantiers, seuils…) du compte.
   *
   * @param id identifiant du compte
   * @param profil nouvelles valeurs
   */
  void mettreAJourProfil(UUID id, ProfilIdentite profil);

  /**
   * @param id identifiant du compte
   * @param actif vrai pour activer, faux pour bloquer la connexion
   */
  void definirActif(UUID id, boolean actif);

  /**
   * Définit un mot de passe temporaire : l'utilisateur devra le changer à la prochaine connexion.
   *
   * @param id identifiant du compte
   * @param motDePasseTemporaire mot de passe temporaire
   */
  void reinitialiserMotDePasse(UUID id, String motDePasseTemporaire);

  /**
   * Attribue exactement un rôle CSE : les autres rôles CSE du compte sont retirés, les rôles
   * techniques (ex. {@code default-roles-cse}) conservés.
   *
   * @param id identifiant du compte
   * @param role rôle à attribuer
   */
  void assignerRole(UUID id, Role role);

  /**
   * Ajoute l'action requise de configuration TOTP (rôle soumis à l'authentification forte).
   *
   * @param id identifiant du compte
   */
  void exigerConfigurationOtp(UUID id);

  /**
   * @param id identifiant du compte
   * @return vrai si un second facteur TOTP est enregistré
   */
  boolean otpConfigure(UUID id);

  /**
   * @return tous les comptes du realm portant au moins un rôle CSE
   */
  List<CompteIdentite> listerComptes();
}
