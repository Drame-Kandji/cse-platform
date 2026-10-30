package sn.cse.fournisseurs.infrastructure.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import sn.cse.fournisseurs.application.port.in.SurveillanceFournisseursUseCase;

/**
 * Job planifié (cron {@code cse.fournisseurs.surveillance.cron}) : signale les documents expirés
 * ({@code DOCUMENT_EXPIRE}) et les scores sous le seuil d'alerte ({@code SCORE_RECALCULE} avec
 * motif).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
    prefix = "cse.fournisseurs.surveillance",
    name = "enabled",
    havingValue = "true")
public class SurveillanceFournisseursJob {

  private static final Logger LOG = LoggerFactory.getLogger(SurveillanceFournisseursJob.class);

  private final SurveillanceFournisseursUseCase surveillance;

  /**
   * @param surveillance cas d'utilisation
   */
  public SurveillanceFournisseursJob(SurveillanceFournisseursUseCase surveillance) {
    this.surveillance = surveillance;
  }

  /** Exécution périodique ; chaque contrôle s'exécute dans sa propre transaction. */
  @Scheduled(
      cron = "${cse.fournisseurs.surveillance.cron}",
      zone = "${cse.fournisseurs.surveillance.zone}")
  public void executer() {
    int documents = surveillance.signalerDocumentsExpires();
    int alertes = surveillance.alerterScoresFaibles();
    LOG.info(
        "Surveillance fournisseurs : {} document(s) expiré(s) signalé(s), {} alerte(s) de score",
        documents,
        alertes);
  }
}
