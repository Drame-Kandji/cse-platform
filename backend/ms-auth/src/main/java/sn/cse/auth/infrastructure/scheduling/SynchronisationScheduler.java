package sn.cse.auth.infrastructure.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import sn.cse.auth.application.exception.IdentiteIndisponibleException;
import sn.cse.auth.application.port.in.SynchroniserUtilisateursUseCase;
import sn.cse.auth.infrastructure.config.AuthProperties;

/**
 * Déclenche la synchronisation Keycloak → profils : une fois l'application prête (avec relances
 * tant que Keycloak est injoignable, sans jamais faire échouer le démarrage), puis périodiquement.
 */
@Component
public class SynchronisationScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SynchronisationScheduler.class);

  private final SynchroniserUtilisateursUseCase synchronisation;
  private final TaskScheduler scheduler;
  private final AuthProperties.Synchronisation config;
  private final Clock clock;
  private final AtomicBoolean enCours = new AtomicBoolean();

  /**
   * @param synchronisation cas d'utilisation
   * @param scheduler planificateur Spring
   * @param properties configuration
   * @param clock horloge
   */
  public SynchronisationScheduler(
      SynchroniserUtilisateursUseCase synchronisation,
      TaskScheduler scheduler,
      AuthProperties properties,
      Clock clock) {
    this.synchronisation = synchronisation;
    this.scheduler = scheduler;
    this.config = properties.synchronisation();
    this.clock = clock;
  }

  /** Planifie les synchronisations une fois l'application démarrée. */
  @EventListener(ApplicationReadyEvent.class)
  public void planifier() {
    Instant maintenant = Instant.now(clock);
    if (config.auDemarrage()) {
      scheduler.schedule(this::synchronisationInitiale, maintenant);
    }
    if (config.periodique()) {
      scheduler.scheduleWithFixedDelay(
          this::tenter, maintenant.plus(config.intervalle()), config.intervalle());
    }
  }

  void synchronisationInitiale() {
    if (!tenter()) {
      LOG.warn("Synchronisation initiale impossible, nouvelle tentative dans {}", config.relance());
      scheduler.schedule(this::synchronisationInitiale, Instant.now(clock).plus(config.relance()));
    }
  }

  /**
   * @return vrai si la synchronisation a abouti (ou si une autre est déjà en cours)
   */
  boolean tenter() {
    if (!enCours.compareAndSet(false, true)) {
      return true;
    }
    try {
      synchronisation.synchroniser();
      return true;
    } catch (IdentiteIndisponibleException e) {
      LOG.warn("Keycloak indisponible pour la synchronisation : {}", e.raison());
      return false;
    } catch (RuntimeException e) {
      LOG.error("Échec de la synchronisation des utilisateurs", e);
      return false;
    } finally {
      enCours.set(false);
    }
  }
}
