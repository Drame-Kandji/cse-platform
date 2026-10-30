package sn.cse.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import sn.cse.common.web.ProblemDetails;

/** Réponses 401/403 au format RFC 7807, cohérentes avec le gestionnaire d'erreurs global. */
public final class ProblemDetailSecurityHandlers {

  private ProblemDetailSecurityHandlers() {}

  /**
   * @param mapper sérialiseur JSON
   * @return point d'entrée renvoyant un 401 ProblemDetail
   */
  public static AuthenticationEntryPoint entryPoint(ObjectMapper mapper) {
    return (request, response, ex) ->
        ecrire(
            response,
            mapper,
            ProblemDetails.of(
                HttpStatus.UNAUTHORIZED,
                "NON_AUTHENTIFIE",
                "Authentification requise",
                "Jeton d'accès absent, invalide ou expiré."));
  }

  /**
   * @param mapper sérialiseur JSON
   * @return gestionnaire renvoyant un 403 ProblemDetail
   */
  public static AccessDeniedHandler accessDenied(ObjectMapper mapper) {
    return (request, response, ex) ->
        ecrire(
            response,
            mapper,
            ProblemDetails.of(
                HttpStatus.FORBIDDEN,
                "ACCES_REFUSE",
                "Accès refusé",
                "Votre rôle ne permet pas cette opération."));
  }

  private static void ecrire(HttpServletResponse response, ObjectMapper mapper, ProblemDetail pd)
      throws IOException {
    response.setStatus(pd.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    mapper.writeValue(response.getOutputStream(), pd);
  }
}
