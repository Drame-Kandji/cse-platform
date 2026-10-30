package sn.cse.besoins.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.cse.besoins.application.service.ParametresBesoins;
import sn.cse.besoins.domain.service.ConsolidationBesoins;
import sn.cse.besoins.domain.validation.ChaineValidation;
import sn.cse.besoins.domain.validation.CritereDeclenchement;
import sn.cse.besoins.domain.validation.EtapeValidation;

/** Assemble les objets du domaine paramétrés par la configuration. */
@Configuration(proxyBeanMethods = false)
public class BesoinsConfiguration {

  /**
   * @param properties configuration
   * @return la chaîne de validation construite à partir des étapes actives
   */
  @Bean
  public ChaineValidation chaineValidation(BesoinsProperties properties) {
    return new ChaineValidation(
        properties.validation().etapes().stream()
            .filter(BesoinsProperties.Etape::actif)
            .map(
                e ->
                    new EtapeValidation(
                        e.code(),
                        e.role(),
                        new CritereDeclenchement(e.quantiteMinimale(), e.prioriteMinimale())))
            .toList());
  }

  /**
   * @param properties configuration
   * @return le service de consolidation
   */
  @Bean
  public ConsolidationBesoins consolidationBesoins(BesoinsProperties properties) {
    return new ConsolidationBesoins(properties.consolidation().fenetreDoublonJours());
  }

  /**
   * @param properties configuration
   * @return les paramètres applicatifs
   */
  @Bean
  public ParametresBesoins parametresBesoins(BesoinsProperties properties) {
    return new ParametresBesoins(properties.consolidation().limite());
  }
}
