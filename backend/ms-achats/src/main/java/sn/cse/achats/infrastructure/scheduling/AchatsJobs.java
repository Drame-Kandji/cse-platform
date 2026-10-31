package sn.cse.achats.infrastructure.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import sn.cse.achats.application.port.in.CloturerAppelOffreUseCase;
import sn.cse.achats.application.port.in.SurveillerLivraisonsUseCase;

/**
 * Traitements planifiés : clôture des appels d'offres à leur date limite et contrôle quotidien des
 * retards de livraison. Les expressions cron sont configurables ({@code -} désactive).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class AchatsJobs {

  private final CloturerAppelOffreUseCase cloture;
  private final SurveillerLivraisonsUseCase livraisons;

  /**
   * @param cloture clôture automatique
   * @param livraisons surveillance des livraisons
   */
  public AchatsJobs(CloturerAppelOffreUseCase cloture, SurveillerLivraisonsUseCase livraisons) {
    this.cloture = cloture;
    this.livraisons = livraisons;
  }

  /** Clôture les appels d'offres publiés dont la date limite est atteinte. */
  @Scheduled(cron = "${cse.achats.jobs.cloture-cron}", zone = "UTC")
  public void cloturerAppelsOffresEchus() {
    cloture.cloturerEchus();
  }

  /** Calcule les pénalités et alerte sur les livraisons en retard. */
  @Scheduled(cron = "${cse.achats.jobs.retard-livraison-cron}", zone = "UTC")
  public void surveillerRetardsLivraison() {
    livraisons.signalerRetards();
  }
}
