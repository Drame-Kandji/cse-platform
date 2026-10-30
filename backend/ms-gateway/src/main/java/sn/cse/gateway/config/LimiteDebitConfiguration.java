package sn.cse.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.cse.gateway.filter.UtilisateurOuIpKeyResolver;

/** Limitation de débit par utilisateur (seau à jetons Redis). */
@Configuration(proxyBeanMethods = false)
public class LimiteDebitConfiguration {

  /**
   * @return clé de limitation : {@code sub} du JWT, à défaut l'adresse IP du client
   */
  @Bean
  public KeyResolver utilisateurOuIpKeyResolver() {
    return new UtilisateurOuIpKeyResolver();
  }

  /**
   * @param properties configuration
   * @return limiteur Redis paramétré (créé seulement si la limitation est active)
   */
  @Bean
  @ConditionalOnProperty(prefix = "cse.gateway.limite-debit", name = "actif", havingValue = "true")
  public RedisRateLimiter cseRedisRateLimiter(GatewayProperties properties) {
    GatewayProperties.LimiteDebit l = properties.limiteDebit();
    return new RedisRateLimiter(l.debit(), l.rafale(), l.jetonsParRequete());
  }
}
