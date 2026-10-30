package sn.cse.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import sn.cse.gateway.config.GatewayProperties;
import sn.cse.gateway.filter.CorrelationIdWebFilter;
import sn.cse.gateway.filter.ReponseLimiteeWebFilter;
import sn.cse.gateway.filter.UtilisateurOuIpKeyResolver;
import sn.cse.gateway.web.ProblemeErrorWebExceptionHandler;
import sn.cse.gateway.web.Problemes;

class ComposantsPasserelleTest {

  private final ObjectMapper mapper =
      Jackson2ObjectMapperBuilder.json()
          .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();
  private final Problemes problemes =
      new Problemes(mapper, Clock.fixed(Instant.parse("2026-10-30T09:15:00Z"), ZoneOffset.UTC));

  @Test
  void should_utiliser_le_sub_puis_l_ip_when_cle_de_limitation() {
    UtilisateurOuIpKeyResolver resolver = new UtilisateurOuIpKeyResolver();
    Principal principal = () -> "00000000-0000-0000-0000-000000000101";
    ServerWebExchange authentifie =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/besoins"))
            .mutate()
            .principal(Mono.just(principal))
            .build();
    assertThat(resolver.resolve(authentifie).block())
        .isEqualTo("utilisateur:00000000-0000-0000-0000-000000000101");

    ServerWebExchange anonyme =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/besoins")
                .remoteAddress(new InetSocketAddress("10.1.2.3", 5555))
                .header("X-Forwarded-For", "1.1.1.1"));
    assertThat(resolver.resolve(anonyme).block()).isEqualTo("ip:10.1.2.3");
  }

  @Test
  void should_ajouter_un_problem_detail_when_reponse_429_sans_corps() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/besoins"));
    exchange.getAttributes().put(CorrelationIdWebFilter.ATTRIBUT, "abc-123");
    WebFilterChain limiteur =
        e -> {
          e.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
          e.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
          return e.getResponse().setComplete();
        };
    new ReponseLimiteeWebFilter(problemes).filter(exchange, limiteur).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(exchange.getResponse().getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"))
        .isEqualTo("0");
    assertThat(exchange.getResponse().getBodyAsString().block())
        .contains("\"code\":\"TROP_DE_REQUETES\"")
        .contains("\"traceId\":\"abc-123\"")
        .contains("\"timestamp\":\"2026-10-30T09:15:00Z\"");
  }

  @Test
  void should_laisser_passer_when_reponse_ordinaire() {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x"));
    WebFilterChain chaine =
        e -> {
          e.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
          return e.getResponse().setComplete();
        };
    new ReponseLimiteeWebFilter(problemes).filter(exchange, chaine).block();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(exchange.getResponse().getBodyAsString().block()).isEmpty();
  }

  @Test
  void should_traduire_les_erreurs_en_problem_detail_when_exception_non_geree() {
    ProblemeErrorWebExceptionHandler handler = new ProblemeErrorWebExceptionHandler(problemes);
    assertThat(code(handler, new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT)))
        .contains("\"code\":\"DELAI_DEPASSE\"")
        .contains("\"status\":504");
    assertThat(code(handler, new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED)))
        .contains("METHODE_NON_AUTORISEE");
    assertThat(code(handler, new ResponseStatusException(HttpStatus.BAD_REQUEST)))
        .contains("REQUETE_INVALIDE");
    assertThat(code(handler, new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)))
        .contains("SERVICE_INDISPONIBLE");
    assertThat(code(handler, new IllegalStateException("détail interne")))
        .contains("ERREUR_INTERNE")
        .doesNotContain("détail interne");
  }

  @Test
  void should_corriger_l_identifiant_de_correlation_when_filtre() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/me")
                .header(CorrelationIdWebFilter.HEADER, "ok_42.a-b"));
    ServerWebExchange[] vu = new ServerWebExchange[1];
    new CorrelationIdWebFilter()
        .filter(
            exchange,
            e -> {
              vu[0] = e;
              return Mono.empty();
            })
        .block();
    assertThat(vu[0].getRequest().getHeaders().getFirst(CorrelationIdWebFilter.HEADER))
        .isEqualTo("ok_42.a-b");
    assertThat(CorrelationIdWebFilter.de(vu[0])).isEqualTo("ok_42.a-b");
    assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdWebFilter.HEADER))
        .isEqualTo("ok_42.a-b");
  }

  @Test
  void should_refuser_le_joker_cors_when_identifiants_autorises() {
    assertThatThrownBy(() -> new GatewayProperties.Cors(List.of("*"), Duration.ofHours(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new GatewayProperties.Cors(null, Duration.ofHours(1)).originesAutorisees())
        .isEmpty();
  }

  private String code(ProblemeErrorWebExceptionHandler handler, Throwable erreur) {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/x"));
    handler.handle(exchange, erreur).block();
    return exchange.getResponse().getBodyAsString().block();
  }
}
