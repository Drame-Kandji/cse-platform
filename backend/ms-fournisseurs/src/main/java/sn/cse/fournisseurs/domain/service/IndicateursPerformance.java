package sn.cse.fournisseurs.domain.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import sn.cse.fournisseurs.domain.model.NotesEvaluation;

/**
 * Indicateurs consolidés servant au calcul du score (R07).
 *
 * @param evaluations notes de toutes les évaluations du fournisseur
 * @param tauxLivraisonALHeure taux de livraison à l'heure 0..100 (absent si aucune livraison)
 * @param litigesOuverts nombre de litiges OUVERT ou EN_COURS
 * @param litigesResolus nombre de litiges RESOLU (les litiges CLASSE ne pénalisent pas)
 */
public record IndicateursPerformance(
    List<NotesEvaluation> evaluations,
    Optional<BigDecimal> tauxLivraisonALHeure,
    int litigesOuverts,
    int litigesResolus) {

  /** Normalise les collections. */
  public IndicateursPerformance {
    evaluations = evaluations == null ? List.of() : List.copyOf(evaluations);
    tauxLivraisonALHeure = tauxLivraisonALHeure == null ? Optional.empty() : tauxLivraisonALHeure;
  }
}
