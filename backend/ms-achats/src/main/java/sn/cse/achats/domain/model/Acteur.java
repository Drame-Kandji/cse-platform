package sn.cse.achats.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Auteur d'une action métier (création, décision, signature).
 *
 * @param id identifiant Keycloak, {@code null} pour une action système
 * @param nom nom affichable
 */
public record Acteur(UUID id, String nom) {

  /** Acteur des traitements automatiques (clôture à date limite, alertes). */
  public static final Acteur SYSTEME = new Acteur(null, "Système");

  public Acteur {
    Objects.requireNonNull(nom, "nom");
  }
}
