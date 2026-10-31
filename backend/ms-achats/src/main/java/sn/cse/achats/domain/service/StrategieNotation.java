package sn.cse.achats.domain.service;

import java.math.BigDecimal;
import java.util.Optional;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.Offre;

/**
 * Stratégie de notation d'une offre sur un critère (patron Strategy). Chaque critère dispose de sa
 * stratégie : calcul relatif à la meilleure offre (PRIX, DELAI) ou reprise d'une note saisie.
 */
public interface StrategieNotation {

  /**
   * @return le critère noté par cette stratégie
   */
  CritereEvaluation critere();

  /**
   * @param offre offre à noter
   * @param contexte meilleures valeurs parmi les offres comparées
   * @return note sur 100 (deux décimales), vide si la note n'est pas encore disponible
   */
  Optional<BigDecimal> noter(Offre offre, ContexteEvaluation contexte);
}
