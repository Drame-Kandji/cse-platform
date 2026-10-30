package sn.cse.common.web;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** Fabrique de ProblemDetail (RFC 7807) enrichis : code stable, traceId, horodatage. */
public final class ProblemDetails {

  /** Base des URI {@code type} des erreurs. */
  public static final String TYPE_BASE = "https://cse.sn/erreurs/";

  private ProblemDetails() {}

  /**
   * @param status statut HTTP
   * @param code code d'erreur stable
   * @param title titre court
   * @param detail message en français destiné à l'utilisateur
   * @return le ProblemDetail
   */
  public static ProblemDetail of(HttpStatusCode status, String code, String title, String detail) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setTitle(title);
    enrichir(pd, code);
    return pd;
  }

  /**
   * Ajoute {@code code}, {@code traceId} et {@code timestamp} à un ProblemDetail existant.
   *
   * @param pd problème à compléter
   * @param code code d'erreur stable
   */
  public static void enrichir(ProblemDetail pd, String code) {
    pd.setType(URI.create(TYPE_BASE + code.toLowerCase(Locale.ROOT).replace('_', '-')));
    pd.setProperty("code", code);
    pd.setProperty("traceId", MDC.get(CorrelationIdFilter.MDC_KEY));
    pd.setProperty("timestamp", Instant.now());
  }
}
