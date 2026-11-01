package sn.cse.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import sn.cse.gateway.filter.IpClientKeyResolver;

/** Limitation de débit des routes publiques (inscription du portail fournisseur). */
class LimiteDebitPubliqueTest {

  @Test
  void should_autoriser_n_requetes_par_minute_rafale_comprise() {
    RedisRateLimiter.Config cinq = RoutesConfiguration.configurationParMinute(5);
    assertThat(cinq.getReplenishRate()).isEqualTo(1);
    assertThat(cinq.getRequestedTokens()).isEqualTo(12);
    assertThat(cinq.getBurstCapacity()).isEqualTo(60);
    // Débit soutenu = 60 s × recharge / coût ; rafale = capacité / coût.
    assertThat(60 / cinq.getRequestedTokens()).isEqualTo(5);
    assertThat(cinq.getBurstCapacity() / cinq.getRequestedTokens()).isEqualTo(5);

    RedisRateLimiter.Config sept = RoutesConfiguration.configurationParMinute(7);
    assertThat(sept.getRequestedTokens()).isEqualTo(9);
    assertThat(sept.getBurstCapacity() / sept.getRequestedTokens()).isEqualTo(7);
    assertThat(RoutesConfiguration.configurationParMinute(60).getRequestedTokens()).isEqualTo(1);
  }

  @Test
  void should_utiliser_toujours_l_ip_avec_une_cle_propre_a_la_route() {
    IpClientKeyResolver resolver = new IpClientKeyResolver("ms-fournisseurs-public-inscription");
    Principal principal = () -> "00000000-0000-0000-0000-000000000701";
    ServerWebExchange avecJeton =
        MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/portail/inscription")
                    .remoteAddress(new InetSocketAddress("10.1.2.3", 5555))
                    .header("X-Forwarded-For", "1.1.1.1"))
            .mutate()
            .principal(Mono.just(principal))
            .build();
    assertThat(resolver.resolve(avecJeton).block())
        .isEqualTo("public:ms-fournisseurs-public-inscription:ip:10.1.2.3");
  }

  @Test
  void should_nommer_la_route_publique_et_lire_sa_methode() {
    GatewayProperties.RoutePublique inscription =
        new GatewayProperties.RoutePublique("/api/portail/inscription", "POST", 5);
    assertThat(RoutesConfiguration.idPublique("ms-fournisseurs", inscription))
        .isEqualTo("ms-fournisseurs-public-inscription");
    assertThat(inscription.methodeHttp()).isEqualTo(HttpMethod.POST);
  }
}
