package sn.cse.suivi.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sn.cse.suivi.application.port.in.DeclencherAlertesUseCase;

/**
 * Planifie les contrôles quotidiens (crons {@code cse.suivi.alertes.*}). Désactivable par {@code
 * cse.suivi.alertes.actives=false} (tests, instances secondaires).
 */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "cse.suivi.alertes", name = "actives", havingValue = "true")
public class AlertesScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(AlertesScheduler.class);

  private final DeclencherAlertesUseCase alertes;

  /**
   * @param alertes cas d'utilisation
   */
  public AlertesScheduler(DeclencherAlertesUseCase alertes) {
    this.alertes = alertes;
  }

  /** Contrôle de la maintenance préventive (heures / jours). */
  @Scheduled(cron = "${cse.suivi.alertes.maintenance-cron}")
  public void maintenancePreventive() {
    LOG.info(
        "Contrôle maintenance préventive : {} alerte(s)", alertes.alerterMaintenancesPreventives());
  }

  /** Contrôle des fins d'affectation proches (J-n). */
  @Scheduled(cron = "${cse.suivi.alertes.fin-affectation-cron}")
  public void finsAffectation() {
    LOG.info("Contrôle fins d'affectation : {} alerte(s)", alertes.alerterFinsAffectation());
  }
}
