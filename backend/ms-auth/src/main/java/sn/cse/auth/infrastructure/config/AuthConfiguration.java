package sn.cse.auth.infrastructure.config;

import java.util.HashSet;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import sn.cse.auth.application.port.out.LienActivation;
import sn.cse.auth.application.service.ParametresPortailFournisseur;
import sn.cse.auth.domain.model.PolitiqueOtp;
import sn.cse.auth.infrastructure.keycloak.KeycloakProperties;
import sn.cse.common.security.CurrentUserProvider;

/** Câblage de l'infrastructure : propriétés, politique OTP, audit JPA, planificateur. */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditeur")
@EnableScheduling
@EnableConfigurationProperties({AuthProperties.class, KeycloakProperties.class})
public class AuthConfiguration {

  /** Auteur technique des modifications sans utilisateur (synchronisation planifiée). */
  static final String SYSTEME = "systeme";

  /**
   * @param properties configuration
   * @return la politique OTP issue de la configuration
   */
  @Bean
  public PolitiqueOtp politiqueOtp(AuthProperties properties) {
    return new PolitiqueOtp(new HashSet<>(properties.otpObligatoire()));
  }

  /**
   * @param properties configuration
   * @return paramètres des comptes du portail fournisseur
   */
  @Bean
  public ParametresPortailFournisseur parametresPortailFournisseur(AuthProperties properties) {
    AuthProperties.PortailFournisseur p = properties.portailFournisseur();
    return new ParametresPortailFournisseur(
        p.prenomContact(), new LienActivation(p.clientId(), p.redirectUri(), p.validiteLien()));
  }

  /**
   * @param users utilisateur courant
   * @return l'auteur des colonnes {@code created_by/updated_by}
   */
  @Bean
  public AuditorAware<String> auditeur(CurrentUserProvider users) {
    return () -> Optional.of(users.current().map(u -> u.id().toString()).orElse(SYSTEME));
  }
}
