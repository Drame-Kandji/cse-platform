package sn.cse.achats.domain.model;

import java.util.Objects;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.exception.Regles;

/**
 * Critère d'évaluation et son poids (en points sur 100).
 *
 * @param code critère
 * @param poids poids strictement positif
 */
public record CriterePondere(CritereEvaluation code, int poids) {

  public CriterePondere {
    Objects.requireNonNull(code, "code");
    Regles.exiger(
        poids > 0 && poids <= 100,
        CodesErreur.PONDERATION_INVALIDE,
        "Le poids du critère " + code + " doit être compris entre 1 et 100.");
  }
}
