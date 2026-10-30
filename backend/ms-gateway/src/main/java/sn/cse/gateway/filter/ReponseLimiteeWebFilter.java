package sn.cse.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import sn.cse.gateway.web.Problemes;

/**
 * Le filtre {@code RequestRateLimiter} de Spring Cloud Gateway termine la réponse 429 sans corps :
 * ce décorateur y ajoute un ProblemDetail ({@code TROP_DE_REQUETES}) cohérent avec les autres
 * erreurs, en conservant les en-têtes {@code X-RateLimit-*}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ReponseLimiteeWebFilter implements WebFilter {

  private final Problemes problemes;

  /**
   * @param problemes fabrique d'erreurs
   */
  public ReponseLimiteeWebFilter(Problemes problemes) {
    this.problemes = problemes;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpResponseDecorator reponse =
        new ServerHttpResponseDecorator(exchange.getResponse()) {
          @Override
          public Mono<Void> setComplete() {
            HttpStatusCode statut = getStatusCode();
            if (statut != null
                && statut.value() == HttpStatus.TOO_MANY_REQUESTS.value()
                && !isCommitted()) {
              return problemes.ecrire(
                  getDelegate(),
                  problemes.creer(
                      HttpStatus.TOO_MANY_REQUESTS,
                      "TROP_DE_REQUETES",
                      "Trop de requêtes",
                      "Vous avez dépassé le nombre de requêtes autorisé. Réessayez dans quelques"
                          + " secondes.",
                      exchange));
            }
            return super.setComplete();
          }
        };
    return chain.filter(exchange.mutate().response(reponse).build());
  }
}
