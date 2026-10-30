package sn.cse.common.client;

import org.slf4j.MDC;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.CorrelationIdFilter;

/**
 * Fabrique de {@link RestClient} pour appeler un autre microservice : délais bornés, relais du JWT
 * de l'utilisateur (l'appelé applique ses propres règles d'accès) et de l'identifiant de
 * corrélation. La résilience (retry, circuit breaker) est appliquée par les adaptateurs appelants.
 */
public class ServiceRestClientFactory {

  private final RestClient.Builder builder;
  private final CurrentUserProvider users;
  private final ServiceClientProperties properties;

  /**
   * @param builder builder Spring Boot (observabilité incluse)
   * @param users utilisateur courant
   * @param properties délais
   */
  public ServiceRestClientFactory(
      RestClient.Builder builder, CurrentUserProvider users, ServiceClientProperties properties) {
    this.builder = builder;
    this.users = users;
    this.properties = properties;
  }

  /**
   * @param baseUrl URL de base du service cible (configuration)
   * @return un client prêt à l'emploi
   */
  public RestClient creer(String baseUrl) {
    return builder
        .clone()
        .baseUrl(baseUrl)
        .requestFactory(
            ClientHttpRequestFactoryBuilder.detect()
                .build(
                    ClientHttpRequestFactorySettings.defaults()
                        .withConnectTimeout(properties.connectTimeout())
                        .withReadTimeout(properties.readTimeout())))
        .requestInterceptor(relais())
        .build();
  }

  private ClientHttpRequestInterceptor relais() {
    return (request, body, execution) -> {
      users
          .currentToken()
          .ifPresent(
              token -> request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + token));
      String correlation = MDC.get(CorrelationIdFilter.MDC_KEY);
      if (correlation != null) {
        request.getHeaders().set(CorrelationIdFilter.HEADER, correlation);
      }
      return execution.execute(request, body);
    };
  }
}
