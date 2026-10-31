package sn.cse.reporting.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.reporting.application.command.DemandeSerie;
import sn.cse.reporting.application.port.in.ConsulterSerieUseCase;
import sn.cse.reporting.application.port.out.LectureReportingRepository;
import sn.cse.reporting.domain.model.Granularite;
import sn.cse.reporting.domain.model.Indicateur;
import sn.cse.reporting.domain.model.ParametresReporting;
import sn.cse.reporting.domain.model.Perimetre;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.Point;
import sn.cse.reporting.domain.service.CalculateurSeries;
import sn.cse.reporting.domain.service.PolitiqueAcces;

/** Séries temporelles des indicateurs, calculées sur les vues et les instantanés du parc. */
@Service
@Transactional(readOnly = true)
public class ConsulterSerieService implements ConsulterSerieUseCase {

  private final LectureReportingRepository lecture;
  private final ParametresReporting parametres;
  private final CalculateurSeries calculateur;
  private final Clock clock;

  /**
   * @param lecture lecture des vues
   * @param parametres paramètres métier
   * @param clock horloge
   */
  public ConsulterSerieService(
      LectureReportingRepository lecture, ParametresReporting parametres, Clock clock) {
    this.lecture = lecture;
    this.parametres = parametres;
    this.calculateur = new CalculateurSeries(parametres);
    this.clock = clock;
  }

  @Override
  public List<Point> serie(DemandeSerie demande) {
    Indicateur indicateur = Indicateur.depuis(demande.indicateur());
    Granularite granularite = granularite(demande.granularite());
    Perimetre perimetre =
        PolitiqueAcces.pourSerie(demande.utilisateur(), indicateur, demande.chantierId());
    LocalDate aujourdhui = LocalDate.now(clock);
    Periode periode =
        Periode.resoudre(
            demande.du(),
            demande.au(),
            aujourdhui,
            parametres.periodeDefautJours(),
            parametres.periodeMaxJours());
    List<sn.cse.reporting.domain.model.vue.ParcInstantane> instantanes =
        indicateur == Indicateur.TAUX_DISPONIBILITE ? lecture.instantanes(periode) : List.of();
    return calculateur.calculer(
        indicateur,
        granularite,
        periode,
        perimetre,
        lecture.charger(periode, perimetre),
        instantanes,
        aujourdhui);
  }

  private static Granularite granularite(String code) {
    if (code == null || code.isBlank()) {
      return Granularite.MOIS;
    }
    try {
      return Granularite.valueOf(code.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new InvalidRequestException(
          "GRANULARITE_INCONNUE",
          "Granularité « " + code + " » inconnue. Valeurs : JOUR, SEMAINE, MOIS.");
    }
  }
}
