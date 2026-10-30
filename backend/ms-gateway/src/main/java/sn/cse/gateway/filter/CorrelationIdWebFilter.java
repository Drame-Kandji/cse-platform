package sn.cse.gateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Identifiant de corrélation {@code X-Correlation-Id} : repris de la requête s'il est valide, sinon
 * généré ; ajouté à la requête relayée aux services, à la réponse et aux attributs de l'échange
 * (utilisé comme {@code traceId} des ProblemDetail). Placé avant la sécurité pour que les 401/403
 * soient aussi corrélés.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

  /** En-tête HTTP de corrélation. */
  public static final String HEADER = "X-Correlation-Id";

  /** Attribut d'échange portant l'identifiant. */
  public static final String ATTRIBUT = CorrelationIdWebFilter.class.getName() + ".id";

  // Même format que cse-common : empêche l'injection de contenu arbitraire dans les journaux.
  private static final Pattern FORMAT_VALIDE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String recu = exchange.getRequest().getHeaders().getFirst(HEADER);
    String id =
        recu != null && FORMAT_VALIDE.matcher(recu).matches() ? recu : UUID.randomUUID().toString();
    ServerHttpRequest requete =
        exchange.getRequest().mutate().headers(h -> h.set(HEADER, id)).build();
    ServerWebExchange echange = exchange.mutate().request(requete).build();
    echange.getAttributes().put(ATTRIBUT, id);
    echange.getResponse().getHeaders().set(HEADER, id);
    return chain.filter(echange);
  }

  /**
   * @param exchange échange courant
   * @return l'identifiant de corrélation de l'échange (ou {@code null} hors filtre)
   */
  public static String de(ServerWebExchange exchange) {
    return exchange.getAttribute(ATTRIBUT);
  }
}
