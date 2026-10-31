package sn.cse.reporting.application.port.out;

import java.util.List;
import sn.cse.reporting.domain.model.DonneesReporting;
import sn.cse.reporting.domain.model.Perimetre;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.vue.ParcInstantane;

/** Lecture des vues pour le calcul des indicateurs. */
public interface LectureReportingRepository {

  /**
   * Charge les vues utiles à la période et au périmètre. Le chargement peut être plus large que
   * nécessaire (le domaine refiltre), jamais plus étroit.
   *
   * @param periode période
   * @param perimetre périmètre
   * @return les vues
   */
  DonneesReporting charger(Periode periode, Perimetre perimetre);

  /**
   * @param periode période
   * @return instantanés du parc de la période, plus le dernier instantané antérieur à son début
   */
  List<ParcInstantane> instantanes(Periode periode);
}
