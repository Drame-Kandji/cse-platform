package sn.cse.fournisseurs.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Évaluation d'un fournisseur (après livraison ou en cours de contrat). Immuable une fois créée.
 *
 * @param id identifiant
 * @param fournisseurId fournisseur évalué
 * @param contratId contrat concerné (optionnel)
 * @param notes notes par critère (0..10)
 * @param noteGlobale note globale pondérée (0..100)
 * @param commentaire commentaire libre
 * @param evaluateur auteur de l'évaluation
 * @param date instant de l'évaluation
 */
public record Evaluation(
    UUID id,
    UUID fournisseurId,
    UUID contratId,
    NotesEvaluation notes,
    BigDecimal noteGlobale,
    String commentaire,
    Auteur evaluateur,
    Instant date) {

  /** Vérifie les champs obligatoires. */
  public Evaluation {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(fournisseurId, "fournisseurId");
    Objects.requireNonNull(notes, "notes");
    Objects.requireNonNull(noteGlobale, "noteGlobale");
    Objects.requireNonNull(evaluateur, "evaluateur");
    Objects.requireNonNull(date, "date");
    commentaire = commentaire == null || commentaire.isBlank() ? null : commentaire.trim();
  }

  /**
   * Crée une nouvelle évaluation.
   *
   * @param fournisseur fournisseur évalué (doit être évaluable)
   * @param contratId contrat concerné (optionnel)
   * @param notes notes par critère
   * @param noteGlobale note globale calculée par la politique de score
   * @param commentaire commentaire
   * @param evaluateur auteur
   * @param date instant
   * @return évaluation
   */
  public static Evaluation creer(
      Fournisseur fournisseur,
      UUID contratId,
      NotesEvaluation notes,
      BigDecimal noteGlobale,
      String commentaire,
      Auteur evaluateur,
      Instant date) {
    fournisseur.verifierEvaluable();
    return new Evaluation(
        UUID.randomUUID(),
        fournisseur.getId(),
        contratId,
        notes,
        noteGlobale,
        commentaire,
        evaluateur,
        date);
  }
}
