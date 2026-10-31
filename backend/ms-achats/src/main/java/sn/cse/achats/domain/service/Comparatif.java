package sn.cse.achats.domain.service;

import java.util.List;
import java.util.UUID;
import sn.cse.achats.domain.model.CriterePondere;

/**
 * Tableau comparatif des offres d'un appel d'offres.
 *
 * @param appelOffreId appel d'offres
 * @param criteres grille de pondération
 * @param offres offres triées par rang (les offres incomplètement notées en dernier)
 * @param recommandationOffreId offre classée première, {@code null} si aucune n'est complètement
 *     notée
 */
public record Comparatif(
    UUID appelOffreId,
    List<CriterePondere> criteres,
    List<OffreEvaluee> offres,
    UUID recommandationOffreId) {

  public Comparatif {
    criteres = List.copyOf(criteres);
    offres = List.copyOf(offres);
  }
}
