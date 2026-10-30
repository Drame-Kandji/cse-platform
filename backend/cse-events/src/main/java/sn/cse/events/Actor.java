package sn.cse.events;

import java.util.List;

/**
 * Auteur d'une action à l'origine d'un événement.
 *
 * @param id identifiant Keycloak ({@code sub}) ou {@code null} pour une action système
 * @param nom nom affichable
 * @param roles rôles détenus au moment de l'action
 */
public record Actor(String id, String nom, List<String> roles) {

  /** Acteur utilisé pour les traitements planifiés ou automatiques. */
  public static final Actor SYSTEME = new Actor(null, "Système", List.of());

  public Actor {
    roles = roles == null ? List.of() : List.copyOf(roles);
  }
}
