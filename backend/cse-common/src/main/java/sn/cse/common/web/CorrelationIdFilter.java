package sn.cse.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Propage l'identifiant de corrélation {@code X-Correlation-Id} : repris de la requête (ou généré),
 * placé dans le MDC des logs, renvoyé dans la réponse et relayé vers les autres services.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

  /** En-tête HTTP de corrélation. */
  public static final String HEADER = "X-Correlation-Id";

  /** Clé MDC (également utilisée comme {@code traceId} des ProblemDetail). */
  public static final String MDC_KEY = "correlationId";

  // Empêche l'injection de contenu arbitraire dans les logs via l'en-tête.
  private static final Pattern FORMAT_VALIDE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String recu = request.getHeader(HEADER);
    String id =
        recu != null && FORMAT_VALIDE.matcher(recu).matches() ? recu : UUID.randomUUID().toString();
    MDC.put(MDC_KEY, id);
    response.setHeader(HEADER, id);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
