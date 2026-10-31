package sn.cse.achats.infrastructure.config;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import sn.cse.achats.application.service.ParametresAchats;
import sn.cse.achats.domain.model.ParametresContrat;
import sn.cse.achats.domain.service.MoteurEvaluation;
import sn.cse.common.security.CurrentUserProvider;

/** Beans métier de ms-achats et audit JPA (auteur et horodatage des modifications). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AchatsProperties.class)
@EnableJpaAuditing(auditorAwareRef = "auditeur", dateTimeProviderRef = "horodatage")
public class AchatsConfiguration {

  /** Auteur enregistré pour les traitements sans utilisateur (jobs, consommateurs, seed). */
  static final String SYSTEME = "systeme";

  /**
   * @param properties configuration {@code cse.achats}
   * @return les paramètres métier transmis aux cas d'utilisation
   */
  @Bean
  public ParametresAchats parametresAchats(AchatsProperties properties) {
    return new ParametresAchats(
        properties.seuilDelegation(),
        new ParametresContrat(
            properties.penaliteRetardPctParJourDefaut(),
            properties.plafondPenalitesPct(),
            properties.tauxTarifJournalier(),
            properties.intervalleRappelRetardJours()));
  }

  /**
   * @return le moteur d'évaluation (une stratégie par critère)
   */
  @Bean
  public MoteurEvaluation moteurEvaluation() {
    return MoteurEvaluation.standard();
  }

  /**
   * @param users utilisateur courant
   * @return l'auteur des modifications (identifiant Keycloak ou « systeme »)
   */
  @Bean
  public AuditorAware<String> auditeur(CurrentUserProvider users) {
    return () -> Optional.of(users.current().map(u -> u.id().toString()).orElse(SYSTEME));
  }

  /**
   * @param clock horloge
   * @return l'horodatage d'audit basé sur l'horloge injectable
   */
  @Bean
  public DateTimeProvider horodatage(Clock clock) {
    return () -> Optional.of(Instant.now(clock));
  }
}
