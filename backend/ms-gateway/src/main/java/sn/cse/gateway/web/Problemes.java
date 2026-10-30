package sn.cse.gateway.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import sn.cse.gateway.filter.CorrelationIdWebFilter;

/**
 * Fabrique et écriture des erreurs RFC 7807 de la passerelle, au même format que les services
 * ({@code type, title, status, detail, code, traceId, timestamp}).
 */
@Component
public class Problemes {

  /** Base des URI {@code type} (identique à cse-common). */
  public static final String TYPE_BASE = "https://cse.sn/erreurs/";

  private final ObjectMapper mapper;
  private final Clock clock;

  /**
   * @param mapper sérialiseur JSON
   * @param clock horloge
   */
  public Problemes(ObjectMapper mapper, Clock clock) {
    this.mapper = mapper;
    this.clock = clock;
  }

  /**
   * @param status statut HTTP
   * @param code code stable
   * @param titre titre court
   * @param detail message français destiné à l'utilisateur
   * @param exchange échange (traceId, instance)
   * @return le ProblemDetail
   */
  public ProblemDetail creer(
      HttpStatusCode status, String code, String titre, String detail, ServerWebExchange exchange) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setTitle(titre);
    pd.setType(URI.create(TYPE_BASE + code.toLowerCase(Locale.ROOT).replace('_', '-')));
    pd.setInstance(URI.create(exchange.getRequest().getPath().value()));
    pd.setProperty("code", code);
    pd.setProperty("traceId", CorrelationIdWebFilter.de(exchange));
    pd.setProperty("timestamp", Instant.now(clock));
    return pd;
  }

  /**
   * Écrit le problème dans la réponse (statut, type de contenu, corps).
   *
   * @param response réponse cible
   * @param pd problème
   * @return fin de l'écriture
   */
  public Mono<Void> ecrire(ServerHttpResponse response, ProblemDetail pd) {
    byte[] corps;
    try {
      corps = mapper.writeValueAsBytes(pd);
    } catch (JsonProcessingException e) {
      return Mono.error(new IllegalStateException("Sérialisation du ProblemDetail impossible", e));
    }
    response.setStatusCode(HttpStatusCode.valueOf(pd.getStatus()));
    response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    response.getHeaders().setContentLength(corps.length);
    return response.writeWith(Mono.just(response.bufferFactory().wrap(corps)));
  }
}
