package sn.cse.achats.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.Offre;

/** Note DELAI = 100 × meilleur délai / délai de l'offre (la plus rapide obtient 100). */
public final class NotationDelai implements StrategieNotation {

  @Override
  public CritereEvaluation critere() {
    return CritereEvaluation.DELAI;
  }

  @Override
  public Optional<BigDecimal> noter(Offre offre, ContexteEvaluation contexte) {
    return Optional.of(
        BigDecimal.valueOf(contexte.meilleurDelai())
            .multiply(MoteurEvaluation.CENT)
            .divide(
                BigDecimal.valueOf(offre.getDelaiLivraisonJours()),
                MoteurEvaluation.ECHELLE,
                RoundingMode.HALF_UP));
  }
}
