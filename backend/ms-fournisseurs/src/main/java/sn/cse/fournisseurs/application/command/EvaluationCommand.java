package sn.cse.fournisseurs.application.command;

import java.util.UUID;
import sn.cse.fournisseurs.domain.model.NotesEvaluation;

/**
 * Évaluation d'un fournisseur.
 *
 * @param contratId contrat concerné (optionnel)
 * @param qualite note qualité 0..10
 * @param delais note délais 0..10
 * @param conformite note conformité 0..10
 * @param sav note SAV 0..10
 * @param prix note prix 0..10
 * @param commentaire commentaire
 */
public record EvaluationCommand(
    UUID contratId,
    int qualite,
    int delais,
    int conformite,
    int sav,
    int prix,
    String commentaire) {

  /**
   * @return notes validées
   */
  public NotesEvaluation notes() {
    return new NotesEvaluation(qualite, delais, conformite, sav, prix);
  }
}
