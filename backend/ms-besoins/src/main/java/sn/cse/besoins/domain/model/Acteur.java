package sn.cse.besoins.domain.model;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Auteur d'une action sur un besoin.
 *
 * @param id identifiant Keycloak ({@code null} pour une action système)
 * @param nom nom affichable
 * @param roles rôles détenus au moment de l'action
 */
public record Acteur(UUID id, String nom, Set<String> roles) {

  private static final String NOM_SYSTEME = "Système";

  public Acteur {
    nom = nom == null || nom.isBlank() ? NOM_SYSTEME : nom;
    roles = roles == null ? Set.of() : Set.copyOf(roles);
  }

  /**
   * @param user utilisateur authentifié
   * @return l'acteur correspondant
   */
  public static Acteur depuis(CurrentUser user) {
    Objects.requireNonNull(user, "user");
    return new Acteur(user.id(), user.nom(), user.roles());
  }

  /**
   * @param nom libellé de l'automate (ex. « ms-suivi »)
   * @return un acteur technique sans rôle
   */
  public static Acteur systeme(String nom) {
    return new Acteur(null, nom, Set.of());
  }

  /**
   * @param role rôle recherché
   * @return vrai si l'acteur détient ce rôle
   */
  public boolean aLeRole(String role) {
    return roles.contains(role);
  }
}
