package sn.cse.achats.domain.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.Offre;

/**
 * Résultat de l'évaluation d'une offre.
 *
 * @param offre offre évaluée
 * @param notes notes disponibles par critère (0..100)
 * @param scoreGlobal score pondéré, {@code null} tant que tous les critères ne sont pas notés
 * @param rang rang parmi les offres complètement notées, {@code null} sinon
 */
public record OffreEvaluee(
    Offre offre, Map<CritereEvaluation, BigDecimal> notes, BigDecimal scoreGlobal, Integer rang) {

  public OffreEvaluee {
    Map<CritereEvaluation, BigDecimal> ordonnees = new EnumMap<>(CritereEvaluation.class);
    ordonnees.putAll(notes);
    notes = Collections.unmodifiableMap(ordonnees);
  }

  /**
   * @param offre offre non évaluable (appel d'offres encore publié)
   * @return une évaluation vide
   */
  public static OffreEvaluee sansNote(Offre offre) {
    return new OffreEvaluee(offre, Map.of(), null, null);
  }
}
