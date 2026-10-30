package sn.cse.gateway.filter;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Clé de limitation de débit : l'utilisateur authentifié ({@code sub}, nom du principal construit
 * par {@code KeycloakJwtConverter}), à défaut l'adresse IP du client. L'en-tête {@code
 * X-Forwarded-For} n'est volontairement pas utilisé (falsifiable).
 */
public class UtilisateurOuIpKeyResolver implements KeyResolver {

  static final String PREFIXE_UTILISATEUR = "utilisateur:";
  static final String PREFIXE_IP = "ip:";
  static final String INCONNU = "inconnu";

  @Override
  public Mono<String> resolve(ServerWebExchange exchange) {
    return exchange
        .getPrincipal()
        .map(Principal::getName)
        .filter(nom -> !nom.isBlank())
        .map(nom -> PREFIXE_UTILISATEUR + nom)
        .switchIfEmpty(Mono.fromSupplier(() -> PREFIXE_IP + adresse(exchange)));
  }

  private static String adresse(ServerWebExchange exchange) {
    return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
        .map(InetSocketAddress::getAddress)
        .map(a -> a.getHostAddress())
        .orElse(INCONNU);
  }
}
