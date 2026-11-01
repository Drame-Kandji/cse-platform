package sn.cse.gateway.filter;

import java.util.Objects;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Clé de limitation des routes publiques : toujours l'adresse IP du client (même si un jeton est
 * présenté), préfixée par la route afin que son seau Redis soit distinct de celui des utilisateurs.
 * L'en-tête {@code X-Forwarded-For} n'est volontairement pas utilisé (falsifiable).
 */
public class IpClientKeyResolver implements KeyResolver {

  private final String prefixe;

  /**
   * @param route identifiant de la route publique
   */
  public IpClientKeyResolver(String route) {
    this.prefixe = "public:" + Objects.requireNonNull(route, "route") + ":ip:";
  }

  @Override
  public Mono<String> resolve(ServerWebExchange exchange) {
    return Mono.fromSupplier(() -> prefixe + UtilisateurOuIpKeyResolver.adresse(exchange));
  }
}
