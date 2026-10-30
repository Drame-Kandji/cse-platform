package sn.cse.gateway.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.RouteMetadataUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

/**
 * Table de routage construite depuis {@link GatewayProperties} : une route par service
 * (coupe-circuit avec repli ProblemDetail, limitation de débit), une route SSE dédiée (sans
 * coupe-circuit ni délai court) et une route de documentation {@code /v3/api-docs/<service>} par
 * service.
 */
@Configuration(proxyBeanMethods = false)
public class RoutesConfiguration {

  /** Ordre de la route SSE : évaluée avant la route générale de son service. */
  static final int ORDRE_SSE = -10;

  /** Suffixe de l'identifiant des routes de documentation. */
  public static final String SUFFIXE_DOCUMENTATION = "-api-docs";

  /** Suffixe de l'identifiant de la route SSE. */
  public static final String SUFFIXE_SSE = "-sse";

  /** Préfixe des chemins de documentation agrégée. */
  public static final String PREFIXE_DOCUMENTATION = "/v3/api-docs/";

  private static final Logger LOG = LoggerFactory.getLogger(RoutesConfiguration.class);
  private static final String SERVEUR_PASSERELLE = "/";

  /**
   * @param builder constructeur de routes
   * @param properties configuration
   * @param limiteur limiteur Redis (présent si la limitation est active)
   * @param cle résolution de la clé de limitation
   * @param mapper sérialiseur JSON (réécriture des documents OpenAPI)
   * @return la table de routage
   */
  @Bean
  public RouteLocator cseRoutes(
      RouteLocatorBuilder builder,
      GatewayProperties properties,
      ObjectProvider<RedisRateLimiter> limiteur,
      KeyResolver cle,
      ObjectMapper mapper) {
    RouteLocatorBuilder.Builder routes = builder.routes();
    GatewayProperties.Sse sse = properties.sse();
    GatewayProperties.Service emetteur = properties.services().get(sse.service());
    if (emetteur == null) {
      throw new IllegalStateException("cse.gateway.sse.service inconnu : " + sse.service());
    }
    routes.route(
        sse.service() + SUFFIXE_SSE,
        r ->
            r.order(ORDRE_SSE)
                .path(sse.chemins().toArray(String[]::new))
                .filters(
                    f ->
                        limiter(f, properties, limiteur, cle)
                            // Désactive la mise en tampon des proxys (nginx) pour le flux.
                            .setResponseHeader("X-Accel-Buffering", "no"))
                .metadata(RouteMetadataUtils.RESPONSE_TIMEOUT_ATTR, sse.delai().toMillis())
                .uri(emetteur.url()));

    for (Map.Entry<String, GatewayProperties.Service> entree : properties.services().entrySet()) {
      String nom = entree.getKey();
      GatewayProperties.Service service = entree.getValue();
      routes.route(
          nom,
          r ->
              r.path(service.chemins().toArray(String[]::new))
                  .filters(
                      f -> coupeCircuit(limiter(f, properties, limiteur, cle), properties, nom))
                  .uri(service.url()));
      if (Boolean.TRUE.equals(service.documentation())) {
        routes.route(
            nom + SUFFIXE_DOCUMENTATION,
            r ->
                r.path(PREFIXE_DOCUMENTATION + nom)
                    .filters(
                        f ->
                            f.setPath("/v3/api-docs")
                                .modifyResponseBody(
                                    String.class,
                                    String.class,
                                    (exchange, corps) -> Mono.justOrEmpty(serveurs(mapper, corps))))
                    .uri(service.url()));
      }
    }
    return routes.build();
  }

  private static GatewayFilterSpec limiter(
      GatewayFilterSpec f,
      GatewayProperties properties,
      ObjectProvider<RedisRateLimiter> limiteur,
      KeyResolver cle) {
    RedisRateLimiter redis = limiteur.getIfAvailable();
    if (!properties.limiteDebit().actif() || redis == null) {
      return f;
    }
    return f.requestRateLimiter(
        c ->
            c.setRateLimiter(redis)
                .setKeyResolver(cle)
                .setDenyEmptyKey(false)
                .setStatusCode(HttpStatus.TOO_MANY_REQUESTS));
  }

  private static GatewayFilterSpec coupeCircuit(
      GatewayFilterSpec f, GatewayProperties properties, String nom) {
    if (!properties.coupeCircuit().actif()) {
      return f;
    }
    return f.circuitBreaker(c -> c.setName(nom).setFallbackUri("forward:/fallback/" + nom));
  }

  /**
   * Remplace les serveurs annoncés par le service (son adresse interne) par la passerelle, afin que
   * « Try it out » passe par la passerelle.
   */
  static String serveurs(ObjectMapper mapper, String corps) {
    if (corps == null) {
      return null;
    }
    try {
      JsonNode document = mapper.readTree(corps);
      if (document instanceof ObjectNode objet) {
        objet.putArray("servers").addObject().put("url", SERVEUR_PASSERELLE);
        return mapper.writeValueAsString(objet);
      }
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      LOG.debug("Document OpenAPI non JSON, transmis sans modification");
    }
    return corps;
  }
}
