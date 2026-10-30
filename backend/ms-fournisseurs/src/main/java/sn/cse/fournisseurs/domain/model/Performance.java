package sn.cse.fournisseurs.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Indicateurs de performance consolidés d'un fournisseur.
 *
 * @param scorePerformance score 0..100 (une décimale) calculé par la {@code ScorePolicy}
 * @param nombreEvaluations nombre d'évaluations prises en compte
 * @param livraisonsTotal nombre de livraisons suivies (date prévue et réelle connues)
 * @param livraisonsALHeure nombre de livraisons réalisées au plus tard à la date prévue
 */
public record Performance(
    BigDecimal scorePerformance,
    int nombreEvaluations,
    int livraisonsTotal,
    int livraisonsALHeure) {

  private static final BigDecimal CENT = BigDecimal.valueOf(100);

  /** Vérifie la cohérence des compteurs. */
  public Performance {
    if (scorePerformance == null) {
      scorePerformance = BigDecimal.ZERO;
    }
    if (nombreEvaluations < 0 || livraisonsTotal < 0 || livraisonsALHeure < 0) {
      throw new IllegalArgumentException("Compteurs de performance négatifs");
    }
    if (livraisonsALHeure > livraisonsTotal) {
      throw new IllegalArgumentException("Plus de livraisons à l'heure que de livraisons");
    }
  }

  /**
   * Performance initiale d'un fournisseur nouvellement référencé.
   *
   * @param scoreInitial score attribué avant toute évaluation
   * @return indicateurs vierges
   */
  public static Performance initiale(BigDecimal scoreInitial) {
    return new Performance(scoreInitial, 0, 0, 0);
  }

  /**
   * Taux de livraison à l'heure en pourcentage (une décimale), absent si aucune livraison suivie.
   *
   * @return taux 0..100
   */
  public Optional<BigDecimal> tauxLivraisonALHeure() {
    if (livraisonsTotal == 0) {
      return Optional.empty();
    }
    return Optional.of(
        BigDecimal.valueOf(livraisonsALHeure)
            .multiply(CENT)
            .divide(BigDecimal.valueOf(livraisonsTotal), 1, RoundingMode.HALF_UP));
  }

  /**
   * @param alHeure livraison effectuée au plus tard à la date prévue
   * @return nouveaux indicateurs incluant la livraison
   */
  public Performance avecLivraison(boolean alHeure) {
    return new Performance(
        scorePerformance,
        nombreEvaluations,
        livraisonsTotal + 1,
        livraisonsALHeure + (alHeure ? 1 : 0));
  }

  /**
   * @param score nouveau score
   * @param evaluations nombre d'évaluations prises en compte
   * @return nouveaux indicateurs
   */
  public Performance avecScore(BigDecimal score, int evaluations) {
    return new Performance(score, evaluations, livraisonsTotal, livraisonsALHeure);
  }
}
