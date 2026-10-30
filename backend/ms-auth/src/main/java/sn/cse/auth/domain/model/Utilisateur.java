package sn.cse.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import sn.cse.auth.domain.exception.AttributProfilInvalideException;

/**
 * Utilisateur de la plateforme (diagramme de classes UML). Classe abstraite : chaque rôle est une
 * sous-classe concrète portant ses attributs spécifiques. Toutes les sous-classes respectent le
 * même contrat (substituables au sens de Liskov) : les attributs qui ne les concernent pas sont
 * ignorés, jamais refusés.
 *
 * <p>L'identifiant est celui du compte Keycloak ({@code sub}) ; aucun mot de passe n'est conservé
 * (ADR-0012).
 */
public abstract sealed class Utilisateur
    permits AdministrateurSysteme,
        ChefDeChantier,
        DAF,
        DirectionGenerale,
        Fournisseur,
        ResponsableAchats,
        ResponsableMateriel {

  private final UUID id;
  private final Instant createdAt;
  private String email;
  private String prenom;
  private String nom;
  private String telephone;
  private boolean actif;
  private boolean otpConfigure;

  /**
   * @param etat état commun à tous les profils
   */
  protected Utilisateur(EtatCommun etat) {
    Objects.requireNonNull(etat, "etat");
    this.id = Objects.requireNonNull(etat.id(), "id");
    this.createdAt = Objects.requireNonNull(etat.createdAt(), "createdAt");
    this.actif = etat.actif();
    this.otpConfigure = etat.otpConfigure();
    modifierIdentite(etat.identite());
  }

  /**
   * @return le rôle realm porté par ce profil
   */
  public abstract Role role();

  /**
   * @return les attributs spécifiques du profil (les autres sont vides)
   */
  public abstract AttributsProfil attributs();

  /**
   * Met à jour les attributs spécifiques ; ceux qui ne concernent pas ce profil sont ignorés.
   *
   * @param attributs nouveaux attributs
   */
  public abstract void modifierAttributs(AttributsProfil attributs);

  /**
   * Met à jour l'identité (nom, prénom, e-mail, téléphone).
   *
   * @param identite nouvelle identité
   */
  public final void modifierIdentite(Identite identite) {
    Objects.requireNonNull(identite, "identite");
    this.email = obligatoire(identite.email(), "email");
    this.prenom = obligatoire(identite.prenom(), "prenom");
    this.nom = obligatoire(identite.nom(), "nom");
    this.telephone =
        identite.telephone() == null || identite.telephone().isBlank()
            ? null
            : identite.telephone().strip();
  }

  /**
   * Change le rôle : le profil devient une instance de la sous-classe correspondante, en conservant
   * identité, statut et date de création.
   *
   * @param nouveauRole rôle cible
   * @param nouveauxAttributs attributs du nouveau profil
   * @return le nouveau profil (ou celui-ci, mis à jour, si le rôle est inchangé)
   */
  public final Utilisateur convertirEn(Role nouveauRole, AttributsProfil nouveauxAttributs) {
    if (nouveauRole == role()) {
      modifierAttributs(nouveauxAttributs);
      return this;
    }
    return Utilisateurs.creer(nouveauRole, etatCommun(), nouveauxAttributs);
  }

  /**
   * @param actif nouveau statut du compte
   */
  public final void definirActif(boolean actif) {
    this.actif = actif;
  }

  /**
   * @param otpConfigure vrai si un second facteur TOTP est enregistré dans Keycloak
   */
  public final void definirOtpConfigure(boolean otpConfigure) {
    this.otpConfigure = otpConfigure;
  }

  /**
   * @param politique politique d'authentification forte
   * @return vrai si ce profil doit configurer un second facteur
   */
  public final boolean otpObligatoire(PolitiqueOtp politique) {
    return politique.estObligatoire(role());
  }

  /**
   * @return un instantané comparable de toutes les données du profil (détection de changement)
   */
  public final Instantane instantane() {
    return new Instantane(role(), identite(), actif, otpConfigure, attributs());
  }

  /**
   * @return état commun (utilisé lors d'un changement de rôle)
   */
  protected final EtatCommun etatCommun() {
    return new EtatCommun(id, identite(), actif, otpConfigure, createdAt);
  }

  /**
   * @return identité courante
   */
  public final Identite identite() {
    return new Identite(email, prenom, nom, telephone);
  }

  /**
   * @return prénom et nom
   */
  public final String nomComplet() {
    return prenom + " " + nom;
  }

  public final UUID id() {
    return id;
  }

  public final String email() {
    return email;
  }

  public final String prenom() {
    return prenom;
  }

  public final String nom() {
    return nom;
  }

  public final String telephone() {
    return telephone;
  }

  public final boolean actif() {
    return actif;
  }

  public final boolean otpConfigure() {
    return otpConfigure;
  }

  public final Instant createdAt() {
    return createdAt;
  }

  private static String obligatoire(String valeur, String champ) {
    if (valeur == null || valeur.isBlank()) {
      throw new AttributProfilInvalideException(champ, "valeur obligatoire");
    }
    return valeur.strip();
  }

  /**
   * Identité d'un utilisateur.
   *
   * @param email adresse e-mail (identifiant de connexion)
   * @param prenom prénom
   * @param nom nom de famille
   * @param telephone téléphone (facultatif)
   */
  public record Identite(String email, String prenom, String nom, String telephone) {}

  /**
   * État commun à tous les profils, utilisé par la fabrique.
   *
   * @param id identifiant Keycloak
   * @param identite identité
   * @param actif compte actif
   * @param otpConfigure second facteur configuré
   * @param createdAt date de création du profil
   */
  public record EtatCommun(
      UUID id, Identite identite, boolean actif, boolean otpConfigure, Instant createdAt) {}

  /**
   * Instantané immuable d'un profil, comparable par valeur.
   *
   * @param role rôle
   * @param identite identité
   * @param actif statut
   * @param otpConfigure second facteur configuré
   * @param attributs attributs spécifiques
   */
  public record Instantane(
      Role role,
      Identite identite,
      boolean actif,
      boolean otpConfigure,
      AttributsProfil attributs) {}
}
