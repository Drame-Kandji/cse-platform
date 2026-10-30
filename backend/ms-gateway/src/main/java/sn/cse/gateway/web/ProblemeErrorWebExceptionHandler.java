package sn.cse.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Erreurs non gérées de la passerelle (route inconnue, délai dépassé, service sans instance…) au
 * format ProblemDetail ; aucun détail technique n'est renvoyé au client. Prioritaire sur le
 * gestionnaire par défaut de Spring Boot (ordre -1).
 */
@Component
@Order(-2)
public class ProblemeErrorWebExceptionHandler implements ErrorWebExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ProblemeErrorWebExceptionHandler.class);

  private final Problemes problemes;

  /**
   * @param problemes fabrique d'erreurs
   */
  public ProblemeErrorWebExceptionHandler(Problemes problemes) {
    this.problemes = problemes;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    if (exchange.getResponse().isCommitted()) {
      return Mono.error(ex);
    }
    HttpStatusCode statut =
        ex instanceof ResponseStatusException rse
            ? rse.getStatusCode()
            : HttpStatus.INTERNAL_SERVER_ERROR;
    if (statut.is5xxServerError()) {
      LOG.error(
          "Erreur passerelle {} sur {} [{}]",
          statut.value(),
          exchange.getRequest().getPath(),
          exchange.getAttributes().get(sn.cse.gateway.filter.CorrelationIdWebFilter.ATTRIBUT),
          ex);
    }
    return problemes.ecrire(exchange.getResponse(), probleme(statut, exchange));
  }

  private org.springframework.http.ProblemDetail probleme(
      HttpStatusCode statut, ServerWebExchange exchange) {
    return switch (statut.value()) {
      case 404 ->
          problemes.creer(
              statut,
              "RESSOURCE_INTROUVABLE",
              "Ressource introuvable",
              "Aucune ressource ne correspond à cette adresse.",
              exchange);
      case 405 ->
          problemes.creer(
              statut,
              "METHODE_NON_AUTORISEE",
              "Méthode non autorisée",
              "Cette méthode HTTP n'est pas acceptée pour cette ressource.",
              exchange);
      case 502, 503 ->
          problemes.creer(
              statut,
              "SERVICE_INDISPONIBLE",
              "Service indisponible",
              "Le service demandé est momentanément indisponible. Réessayez dans quelques instants.",
              exchange);
      case 504 ->
          problemes.creer(
              statut,
              "DELAI_DEPASSE",
              "Délai dépassé",
              "Le service n'a pas répondu à temps. Réessayez dans quelques instants.",
              exchange);
      default ->
          statut.is4xxClientError()
              ? problemes.creer(
                  statut,
                  "REQUETE_INVALIDE",
                  "Requête invalide",
                  "La requête est invalide.",
                  exchange)
              : problemes.creer(
                  HttpStatus.INTERNAL_SERVER_ERROR,
                  "ERREUR_INTERNE",
                  "Erreur interne",
                  "Une erreur inattendue est survenue. Communiquez le traceId au support.",
                  exchange);
    };
  }
}
