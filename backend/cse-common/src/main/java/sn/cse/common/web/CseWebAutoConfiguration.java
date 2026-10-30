package sn.cse.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/** Composants web communs aux services servlet : erreurs RFC 7807, corrélation, pagination. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(PaginationProperties.class)
@Import(GlobalExceptionHandler.class)
public class CseWebAutoConfiguration {

  /**
   * @return filtre de corrélation placé avant la sécurité
   */
  @Bean
  public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
    FilterRegistrationBean<CorrelationIdFilter> reg =
        new FilterRegistrationBean<>(new CorrelationIdFilter());
    reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return reg;
  }

  /**
   * @param properties bornes de pagination
   * @return fabrique de pages
   */
  @Bean
  @ConditionalOnMissingBean
  public PageableFactory pageableFactory(PaginationProperties properties) {
    return new PageableFactory(properties);
  }

  /** Gestion des erreurs de persistance, uniquement si Spring ORM est présent. */
  @ConditionalOnClass(name = "org.springframework.orm.ObjectOptimisticLockingFailureException")
  @Import(PersistenceExceptionHandler.class)
  static class Persistance {}
}
