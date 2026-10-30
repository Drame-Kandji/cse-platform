package sn.cse.fournisseurs.domain.service;

import java.math.BigDecimal;
import sn.cse.fournisseurs.domain.model.NotesEvaluation;

/**
 * Stratégie de calcul du score de performance fournisseur (règle R07). L'implémentation par défaut
 * est {@link ScorePondere} ; d'autres politiques peuvent être substituées sans toucher aux cas
 * d'utilisation.
 */
public interface ScorePolicy {

  /**
   * Calcule la note globale (0..100) d'une évaluation.
   *
   * @param notes notes par critère (0..10)
   * @return note globale, une décimale
   */
  BigDecimal noteGlobale(NotesEvaluation notes);

  /**
   * Calcule le score de performance (0..100) à partir des indicateurs consolidés.
   *
   * @param indicateurs évaluations, ponctualité et litiges
   * @return score borné 0..100, une décimale
   */
  BigDecimal calculer(IndicateursPerformance indicateurs);

  /**
   * @param score score de performance
   * @return {@code true} si le score est strictement inférieur au seuil d'alerte
   */
  boolean estSousSeuilAlerte(BigDecimal score);

  /**
   * @return seuil d'alerte (0..100)
   */
  BigDecimal seuilAlerte();

  /**
   * @return score attribué à un fournisseur sans évaluation ni livraison
   */
  BigDecimal scoreInitial();
}
