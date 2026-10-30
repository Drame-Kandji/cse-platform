package sn.cse.suivi.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import sn.cse.suivi.domain.exception.SuiviErreurs;

/**
 * Intervalle de dates inclusif {@code [debut, fin]}.
 *
 * @param debut premier jour
 * @param fin dernier jour (≥ début)
 */
public record Periode(LocalDate debut, LocalDate fin) {

  /** Code d'erreur d'une période incohérente. */
  public static final String PERIODE_INVALIDE = "PERIODE_INVALIDE";

  public Periode {
    Objects.requireNonNull(debut, "debut");
    Objects.requireNonNull(fin, "fin");
    if (fin.isBefore(debut)) {
      throw SuiviErreurs.regle(
          PERIODE_INVALIDE,
          "La date de fin (" + fin + ") précède la date de début (" + debut + ").");
    }
  }

  /**
   * @param autre autre période
   * @return vrai si les deux périodes ont au moins un jour commun
   */
  public boolean chevauche(Periode autre) {
    return !debut.isAfter(autre.fin) && !autre.debut.isAfter(fin);
  }

  /**
   * @return nombre de jours couverts (bornes incluses)
   */
  public long dureeJours() {
    return ChronoUnit.DAYS.between(debut, fin) + 1;
  }
}
