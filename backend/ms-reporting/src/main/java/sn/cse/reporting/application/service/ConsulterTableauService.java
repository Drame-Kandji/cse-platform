package sn.cse.reporting.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.reporting.application.command.DemandeTableau;
import sn.cse.reporting.application.port.in.ConsulterTableauUseCase;
import sn.cse.reporting.application.port.out.LectureReportingRepository;
import sn.cse.reporting.domain.model.ParametresReporting;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.Tableau;
import sn.cse.reporting.domain.service.CalculateurTableau;
import sn.cse.reporting.domain.service.PolitiqueAcces;
import sn.cse.reporting.domain.service.PolitiqueAcces.Acces;

/**
 * Tableau de bord : contrôle d'accès (profil, périmètre), résolution de la période, chargement des
 * vues puis calcul pur. Aucun appel à un autre service (D3).
 */
@Service
@Transactional(readOnly = true)
public class ConsulterTableauService implements ConsulterTableauUseCase {

  private final LectureReportingRepository lecture;
  private final ParametresReporting parametres;
  private final CalculateurTableau calculateur;
  private final Clock clock;

  /**
   * @param lecture lecture des vues
   * @param parametres paramètres métier
   * @param clock horloge
   */
  public ConsulterTableauService(
      LectureReportingRepository lecture, ParametresReporting parametres, Clock clock) {
    this.lecture = lecture;
    this.parametres = parametres;
    this.calculateur = new CalculateurTableau(parametres);
    this.clock = clock;
  }

  @Override
  public Tableau tableau(DemandeTableau demande) {
    Acces acces =
        PolitiqueAcces.pourTableau(demande.utilisateur(), demande.profil(), demande.chantierId());
    LocalDate aujourdhui = LocalDate.now(clock);
    Periode periode =
        Periode.resoudre(
            demande.du(),
            demande.au(),
            aujourdhui,
            parametres.periodeDefautJours(),
            parametres.periodeMaxJours());
    return calculateur.calculer(
        acces.profil(),
        periode,
        acces.perimetre(),
        lecture.charger(periode, acces.perimetre()),
        aujourdhui,
        Instant.now(clock));
  }
}
