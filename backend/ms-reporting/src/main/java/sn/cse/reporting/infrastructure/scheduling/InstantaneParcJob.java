package sn.cse.reporting.infrastructure.scheduling;

import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sn.cse.reporting.application.port.in.PhotographierParcUseCase;

/**
 * Instantané quotidien du parc : garantit un point par jour à la série de disponibilité même
 * lorsqu'aucun événement ne modifie le parc ce jour-là.
 */
@Component
public class InstantaneParcJob {

  private static final Logger LOG = LoggerFactory.getLogger(InstantaneParcJob.class);

  private final PhotographierParcUseCase photographe;
  private final Clock clock;

  /**
   * @param photographe cas d'utilisation d'instantané
   * @param clock horloge
   */
  public InstantaneParcJob(PhotographierParcUseCase photographe, Clock clock) {
    this.photographe = photographe;
    this.clock = clock;
  }

  /** Photographie le parc du jour (cron {@code cse.reporting.instantane-cron}, UTC). */
  @Scheduled(cron = "${cse.reporting.instantane-cron}", zone = "UTC")
  public void photographierAujourdhui() {
    LocalDate jour = LocalDate.now(clock);
    photographe.photographier(jour);
    LOG.info("Instantané du parc du {} enregistré", jour);
  }
}
