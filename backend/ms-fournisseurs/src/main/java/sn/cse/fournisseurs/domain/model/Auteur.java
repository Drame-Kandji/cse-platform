package sn.cse.fournisseurs.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Auteur d'une action métier (utilisateur Keycloak ou système).
 *
 * @param id identifiant Keycloak ({@code null} pour le système)
 * @param nom nom affiché
 */
public record Auteur(UUID id, String nom) {

  /** Auteur utilisé pour les traitements automatiques (jobs, événements). */
  public static final Auteur SYSTEME = new Auteur(null, "Système");

  /** Valide la présence du nom. */
  public Auteur {
    Objects.requireNonNull(nom, "nom");
  }
}
