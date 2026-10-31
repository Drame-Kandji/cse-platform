package sn.cse.achats.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.Offre;

/** Note PRIX = 100 × meilleur prix / prix de l'offre (la moins chère obtient 100). */
public final class NotationPrix implements StrategieNotation {

  @Override
  public CritereEvaluation critere() {
    return CritereEvaluation.PRIX;
  }

  @Override
  public Optional<BigDecimal> noter(Offre offre, ContexteEvaluation contexte) {
    return Optional.of(
        contexte
            .meilleurPrix()
            .multiply(MoteurEvaluation.CENT)
            .divide(offre.getMontantPropose(), MoteurEvaluation.ECHELLE, RoundingMode.HALF_UP));
  }
}
