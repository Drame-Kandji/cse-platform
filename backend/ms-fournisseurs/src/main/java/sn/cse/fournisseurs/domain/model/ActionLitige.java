package sn.cse.fournisseurs.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entrée de la chronologie d'un litige.
 *
 * @param date instant de l'action
 * @param auteurId identifiant de l'auteur (optionnel pour le système)
 * @param auteurNom nom de l'auteur
 * @param type nature de l'action
 * @param commentaire description de l'action
 */
public record ActionLitige(
    Instant date, UUID auteurId, String auteurNom, TypeActionLitige type, String commentaire) {

  /** Vérifie les champs obligatoires. */
  public ActionLitige {
    Objects.requireNonNull(date, "date");
    Objects.requireNonNull(auteurNom, "auteurNom");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(commentaire, "commentaire");
  }
}
