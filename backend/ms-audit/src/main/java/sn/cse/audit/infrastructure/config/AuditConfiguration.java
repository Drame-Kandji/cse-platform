package sn.cse.audit.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.cse.audit.domain.model.ParametresAudit;

/** Traduit la configuration externe en paramètres métier purs. */
@Configuration(proxyBeanMethods = false)
public class AuditConfiguration {

  /**
   * @param properties configuration {@code cse.audit.*}
   * @return les paramètres de consultation du journal
   */
  @Bean
  public ParametresAudit parametresAudit(AuditProperties properties) {
    return new ParametresAudit(
        properties.fenetreStatistiquesJours(), properties.longueurMaxRecherche());
  }
}
