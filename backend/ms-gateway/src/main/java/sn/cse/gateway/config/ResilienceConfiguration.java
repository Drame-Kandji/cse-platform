package sn.cse.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Paramètres par défaut des coupe-circuits (un par route de service). */
@Configuration(proxyBeanMethods = false)
public class ResilienceConfiguration {

  /**
   * @param properties configuration
   * @return la configuration par défaut des coupe-circuits réactifs
   */
  @Bean
  public Customizer<ReactiveResilience4JCircuitBreakerFactory> coupeCircuitParDefaut(
      GatewayProperties properties) {
    GatewayProperties.CoupeCircuit c = properties.coupeCircuit();
    return factory ->
        factory.configureDefault(
            id ->
                new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(
                        CircuitBreakerConfig.custom()
                            .slidingWindowSize(c.fenetre())
                            .minimumNumberOfCalls(Math.min(c.fenetre(), 10))
                            .failureRateThreshold(c.seuilEchec())
                            .waitDurationInOpenState(c.attenteOuvert())
                            .build())
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom().timeoutDuration(c.delai()).build())
                    .build());
  }
}
