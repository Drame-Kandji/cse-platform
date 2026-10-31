package sn.cse.achats.application.service;

import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.in.SurveillerLivraisonsUseCase;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.events.EventTypes;

/**
 * Contrôle quotidien des livraisons : met à jour les pénalités courues des contrats en retard et
 * publie {@code ALERTE_RETARD_LIVRAISON} (au plus une fois par intervalle de rappel).
 */
@Service
@Transactional
public class SurveillanceLivraisonsService implements SurveillerLivraisonsUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(SurveillanceLivraisonsService.class);

  private final ContratRepository contrats;
  private final AchatsEventPublisher evenements;
  private final ParametresAchats parametres;
  private final Clock clock;

  /**
   * @param contrats dépôt des contrats
   * @param evenements publication d'événements
   * @param parametres pénalités et rappels
   * @param clock horloge
   */
  public SurveillanceLivraisonsService(
      ContratRepository contrats,
      AchatsEventPublisher evenements,
      ParametresAchats parametres,
      Clock clock) {
    this.contrats = contrats;
    this.evenements = evenements;
    this.parametres = parametres;
    this.clock = clock;
  }

  @Override
  public int signalerRetards() {
    LocalDate aujourdhui = LocalDate.now(clock);
    int alertes = 0;
    for (ContratAchat contrat : contrats.enExecutionEcheanceDepassee(aujourdhui)) {
      boolean alerte = contrat.signalerRetard(aujourdhui, parametres.contrat());
      ContratAchat enregistre = contrats.enregistrer(contrat);
      if (alerte) {
        evenements.contrat(EventTypes.ALERTE_RETARD_LIVRAISON, enregistre);
        alertes++;
      }
    }
    if (alertes > 0) {
      LOG.info("{} alerte(s) de retard de livraison publiée(s)", alertes);
    }
    return alertes;
  }
}
