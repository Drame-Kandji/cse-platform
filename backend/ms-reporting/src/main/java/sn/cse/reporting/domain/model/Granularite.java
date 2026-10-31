package sn.cse.reporting.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;

/** Pas des séries temporelles. */
public enum Granularite {
  /** Un point par jour, libellé {@code 2026-07-14}. */
  JOUR {
    @Override
    LocalDate debut(LocalDate date) {
      return date;
    }

    @Override
    LocalDate suivant(LocalDate debut) {
      return debut.plusDays(1);
    }

    @Override
    String libelle(LocalDate debut) {
      return debut.toString();
    }
  },
  /** Un point par semaine ISO (lundi → dimanche), libellé {@code 2026-W29}. */
  SEMAINE {
    @Override
    LocalDate debut(LocalDate date) {
      return date.with(DayOfWeek.MONDAY);
    }

    @Override
    LocalDate suivant(LocalDate debut) {
      return debut.plusWeeks(1);
    }

    @Override
    String libelle(LocalDate debut) {
      // L'année ISO de la semaine peut différer de l'année civile (ex. 2026-12-28 → 2026-W53).
      return String.format(
          "%d-W%02d",
          debut.get(IsoFields.WEEK_BASED_YEAR), debut.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }
  },
  /** Un point par mois civil, libellé {@code 2026-07}. */
  MOIS {
    @Override
    LocalDate debut(LocalDate date) {
      return date.withDayOfMonth(1);
    }

    @Override
    LocalDate suivant(LocalDate debut) {
      return debut.plusMonths(1);
    }

    @Override
    String libelle(LocalDate debut) {
      return String.format("%d-%02d", debut.getYear(), debut.getMonthValue());
    }
  };

  /**
   * @param date date quelconque
   * @return premier jour de la tranche contenant la date
   */
  abstract LocalDate debut(LocalDate date);

  /**
   * @param debut premier jour d'une tranche
   * @return premier jour de la tranche suivante
   */
  abstract LocalDate suivant(LocalDate debut);

  /**
   * @param debut premier jour d'une tranche
   * @return libellé de la tranche
   */
  abstract String libelle(LocalDate debut);
}
