package sn.cse.suivi.infrastructure.config;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.suivi.application.config.SuiviParametres;

/** Paramètres métier et audit JPA (auteur = utilisateur courant, date = horloge). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SuiviParametres.class)
@EnableJpaAuditing(auditorAwareRef = "auditeur", dateTimeProviderRef = "horodatage")
public class SuiviConfiguration {

  /** Auteur enregistré pour les traitements sans utilisateur (jobs, Kafka, amorçage). */
  static final String SYSTEME = "systeme";

  /**
   * @param utilisateurs utilisateur courant
   * @return l'auteur des modifications (identifiant Keycloak ou {@code systeme})
   */
  @Bean
  public AuditorAware<String> auditeur(CurrentUserProvider utilisateurs) {
    return () ->
        Optional.of(utilisateurs.current().map(u -> u.id()).map(UUID::toString).orElse(SYSTEME));
  }

  /**
   * @param clock horloge
   * @return la source des dates d'audit
   */
  @Bean
  public DateTimeProvider horodatage(Clock clock) {
    return () -> Optional.of(Instant.now(clock));
  }
}
