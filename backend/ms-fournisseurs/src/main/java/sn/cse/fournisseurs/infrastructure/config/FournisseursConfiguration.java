package sn.cse.fournisseurs.infrastructure.config;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.fournisseurs.domain.service.ScorePolicy;
import sn.cse.fournisseurs.domain.service.ScorePondere;

/** Câblage des beans du service : politique de score, audit JPA. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FournisseursProperties.class)
@EnableJpaAuditing(auditorAwareRef = "auditeurCourant", dateTimeProviderRef = "horodatageAudit")
public class FournisseursConfiguration {

  /** Auteur technique inscrit dans les colonnes d'audit hors requête authentifiée. */
  static final String AUDITEUR_SYSTEME = "systeme";

  /**
   * Stratégie de score configurée (R07).
   *
   * @param properties paramètres validés
   * @return politique de score pondéré
   */
  @Bean
  public ScorePolicy scorePolicy(FournisseursProperties properties) {
    return new ScorePondere(properties.score().versParametres());
  }

  /**
   * @param utilisateurs fournisseur de l'utilisateur courant
   * @return identifiant de l'utilisateur courant pour {@code created_by/updated_by}
   */
  @Bean
  public AuditorAware<String> auditeurCourant(Optional<CurrentUserProvider> utilisateurs) {
    return () ->
        Optional.of(
            utilisateurs
                .flatMap(CurrentUserProvider::current)
                .map(u -> u.id().toString())
                .orElse(AUDITEUR_SYSTEME));
  }

  /**
   * @param clock horloge applicative
   * @return horodatage d'audit aligné sur l'horloge
   */
  @Bean
  public DateTimeProvider horodatageAudit(Clock clock) {
    return () -> Optional.of(Instant.now(clock));
  }
}
