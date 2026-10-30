package sn.cse.gateway.api;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import sn.cse.gateway.web.Problemes;

/**
 * Repli des coupe-circuits : atteint uniquement par redirection interne ({@code forward:}) quand un
 * service est injoignable, en échec ou trop lent.
 */
@Hidden
@RestController
public class FallbackController {

  private final Problemes problemes;

  /**
   * @param problemes fabrique d'erreurs
   */
  public FallbackController(Problemes problemes) {
    this.problemes = problemes;
  }

  /**
   * @param service service indisponible
   * @param exchange échange courant
   * @return 503 {@code SERVICE_INDISPONIBLE}
   */
  @RequestMapping("/fallback/{service}")
  public ResponseEntity<ProblemDetail> indisponible(
      @PathVariable String service, ServerWebExchange exchange) {
    ProblemDetail pd =
        problemes.creer(
            HttpStatus.SERVICE_UNAVAILABLE,
            "SERVICE_INDISPONIBLE",
            "Service indisponible",
            "Le service « "
                + service
                + " » est momentanément indisponible. Réessayez dans quelques instants.",
            exchange);
    pd.setProperty("service", service);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(pd);
  }
}
