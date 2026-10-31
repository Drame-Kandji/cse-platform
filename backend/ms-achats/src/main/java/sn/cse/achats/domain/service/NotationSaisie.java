package sn.cse.achats.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.Offre;

/** Reprend la note saisie par l'évaluateur (QUALITE_TECHNIQUE, GARANTIE, SAV, REFERENCES). */
public final class NotationSaisie implements StrategieNotation {

  private final CritereEvaluation critere;

  /**
   * @param critere critère saisi (non calculé)
   */
  public NotationSaisie(CritereEvaluation critere) {
    this.critere = Objects.requireNonNull(critere, "critere");
    if (critere.estCalcule()) {
      throw new IllegalArgumentException("Critère calculé : " + critere);
    }
  }

  @Override
  public CritereEvaluation critere() {
    return critere;
  }

  @Override
  public Optional<BigDecimal> noter(Offre offre, ContexteEvaluation contexte) {
    return offre
        .noteSaisie(critere)
        .map(n -> BigDecimal.valueOf(n).setScale(MoteurEvaluation.ECHELLE, RoundingMode.HALF_UP));
  }
}
