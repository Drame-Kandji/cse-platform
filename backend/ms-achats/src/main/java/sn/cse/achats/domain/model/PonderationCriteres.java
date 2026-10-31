package sn.cse.achats.domain.model;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.exception.Regles;

/**
 * Grille de pondération d'un appel d'offres : critères distincts dont la somme des poids vaut
 * exactement 100.
 *
 * @param criteres critères pondérés, dans l'ordre de saisie
 */
public record PonderationCriteres(List<CriterePondere> criteres) {

  /** Somme attendue des poids. */
  public static final int TOTAL = 100;

  public PonderationCriteres {
    Regles.exiger(
        criteres != null && !criteres.isEmpty(),
        CodesErreur.PONDERATION_INVALIDE,
        "Au moins un critère d'évaluation est requis.");
    criteres = List.copyOf(criteres);
    Set<CritereEvaluation> vus = EnumSet.noneOf(CritereEvaluation.class);
    int somme = 0;
    for (CriterePondere c : criteres) {
      Regles.exiger(
          vus.add(c.code()),
          CodesErreur.PONDERATION_INVALIDE,
          "Le critère " + c.code() + " est présent plusieurs fois.");
      somme += c.poids();
    }
    Regles.exiger(
        somme == TOTAL,
        CodesErreur.PONDERATION_INVALIDE,
        "La somme des poids des critères doit valoir 100 (actuellement " + somme + ").");
  }

  /**
   * @param critere critère recherché
   * @return son poids s'il est retenu
   */
  public Optional<Integer> poids(CritereEvaluation critere) {
    return criteres.stream()
        .filter(c -> c.code() == critere)
        .map(CriterePondere::poids)
        .findFirst();
  }

  /**
   * @param critere critère recherché
   * @return vrai si le critère fait partie de la grille
   */
  public boolean contient(CritereEvaluation critere) {
    return poids(critere).isPresent();
  }
}
