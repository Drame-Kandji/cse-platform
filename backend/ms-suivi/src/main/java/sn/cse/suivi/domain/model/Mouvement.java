package sn.cse.suivi.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entrée immuable de l'historique d'un matériel. Les noms de chantiers sont figés au moment du
 * mouvement (trace historique).
 *
 * @param id identifiant
 * @param materielId matériel
 * @param type nature du mouvement
 * @param chantierOrigineId chantier quitté
 * @param chantierOrigineNom nom du chantier quitté
 * @param chantierDestinationId chantier rejoint
 * @param chantierDestinationNom nom du chantier rejoint
 * @param date instant du mouvement
 * @param auteurId auteur (null pour le système)
 * @param auteurNom nom de l'auteur
 * @param commentaire commentaire libre
 */
public record Mouvement(
    UUID id,
    UUID materielId,
    TypeMouvement type,
    UUID chantierOrigineId,
    String chantierOrigineNom,
    UUID chantierDestinationId,
    String chantierDestinationNom,
    Instant date,
    UUID auteurId,
    String auteurNom,
    String commentaire) {

  public Mouvement {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(materielId, "materielId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(date, "date");
  }

  /**
   * Auteur d'un mouvement.
   *
   * @param id identifiant (null pour le système)
   * @param nom nom affichable
   */
  public record Auteur(UUID id, String nom) {}

  /**
   * @param materielId matériel
   * @param type nature
   * @param origine chantier quitté (optionnel)
   * @param destination chantier rejoint (optionnel)
   * @param date instant
   * @param auteur auteur
   * @param commentaire commentaire
   * @return le mouvement
   */
  public static Mouvement de(
      UUID materielId,
      TypeMouvement type,
      Chantier origine,
      Chantier destination,
      Instant date,
      Auteur auteur,
      String commentaire) {
    return new Mouvement(
        UUID.randomUUID(),
        materielId,
        type,
        origine == null ? null : origine.getId(),
        origine == null ? null : origine.getNom(),
        destination == null ? null : destination.getId(),
        destination == null ? null : destination.getNom(),
        date,
        auteur.id(),
        auteur.nom(),
        commentaire);
  }
}
