package sn.cse.reporting.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import sn.cse.reporting.domain.model.ParametresReporting;

/** Traduit la configuration externe en paramètres métier purs ; active la planification. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ReportingConfiguration {

  /**
   * @param p configuration {@code cse.reporting.*}
   * @return les paramètres des calculs
   */
  @Bean
  public ParametresReporting parametresReporting(ReportingProperties p) {
    return new ParametresReporting(
        p.periodeDefautJours(),
        p.periodeMaxJours(),
        p.maxPointsSerie(),
        p.topFournisseurs(),
        p.dureeChantierDefautJours());
  }
}
