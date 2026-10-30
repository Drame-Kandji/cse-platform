package sn.cse.common.web;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import sn.cse.common.exception.CseException;

/**
 * Traduction unique des exceptions en ProblemDetail RFC 7807. Les erreurs techniques ne révèlent
 * jamais de détail interne au client : elles sont journalisées avec le {@code traceId}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * @param ex exception métier
   * @return ProblemDetail au statut porté par l'exception
   */
  @ExceptionHandler(CseException.class)
  public ResponseEntity<ProblemDetail> metier(CseException ex) {
    HttpStatus status = HttpStatus.valueOf(ex.status());
    return reponse(ProblemDetails.of(status, ex.getCode(), titre(status), ex.getMessage()));
  }

  /**
   * @return 403 ProblemDetail
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> accesRefuse() {
    return reponse(
        ProblemDetails.of(
            HttpStatus.FORBIDDEN,
            "ACCES_REFUSE",
            "Accès refusé",
            "Votre rôle ne permet pas cette opération."));
  }

  /**
   * @param ex violations de contraintes sur paramètres
   * @return 400 ProblemDetail avec la liste des erreurs
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> contraintes(ConstraintViolationException ex) {
    ProblemDetail pd =
        ProblemDetails.of(
            HttpStatus.BAD_REQUEST,
            "VALIDATION",
            "Données invalides",
            "Certains paramètres sont invalides.");
    pd.setProperty(
        "errors",
        ex.getConstraintViolations().stream()
            .map(v -> erreur(dernierSegment(v.getPropertyPath().toString()), v.getMessage()))
            .toList());
    return reponse(pd);
  }

  /**
   * @param ex paramètre de type incorrect (ex. UUID mal formé)
   * @return 400 ProblemDetail
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> typeIncorrect(MethodArgumentTypeMismatchException ex) {
    return reponse(
        ProblemDetails.of(
            HttpStatus.BAD_REQUEST,
            "PARAMETRE_INVALIDE",
            "Paramètre invalide",
            "Valeur invalide pour le paramètre « " + ex.getName() + " »."));
  }

  /**
   * @param ex erreur inattendue
   * @return 500 ProblemDetail générique
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> inattendue(Exception ex) {
    LOG.error("Erreur interne non gérée", ex);
    return reponse(
        ProblemDetails.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "ERREUR_INTERNE",
            "Erreur interne",
            "Une erreur inattendue est survenue. Communiquez le traceId au support."));
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail pd =
        ProblemDetails.of(
            HttpStatus.BAD_REQUEST,
            "VALIDATION",
            "Données invalides",
            "Certains champs du formulaire sont invalides.");
    List<Map<String, String>> erreurs =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> erreur(e.getField(), e.getDefaultMessage()))
            .toList();
    pd.setProperty("errors", erreurs);
    return ResponseEntity.badRequest().body(pd);
  }

  // Les exceptions MVC standard (405, 415, corps illisible…) reçoivent aussi code/traceId.
  @Override
  protected ResponseEntity<Object> createResponseEntity(
      Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
    if (body instanceof ProblemDetail pd && pd.getProperties() == null) {
      boolean introuvable = statusCode.value() == HttpStatus.NOT_FOUND.value();
      ProblemDetails.enrichir(pd, introuvable ? "RESSOURCE_INTROUVABLE" : "REQUETE_INVALIDE");
    }
    return super.createResponseEntity(body, headers, statusCode, request);
  }

  private static ResponseEntity<ProblemDetail> reponse(ProblemDetail pd) {
    return ResponseEntity.status(pd.getStatus()).body(pd);
  }

  private static Map<String, String> erreur(String champ, String message) {
    return Map.of("field", champ, "message", message == null ? "invalide" : message);
  }

  private static String dernierSegment(String chemin) {
    int i = chemin.lastIndexOf('.');
    return i < 0 ? chemin : chemin.substring(i + 1);
  }

  private static String titre(HttpStatus status) {
    return switch (status) {
      case NOT_FOUND -> "Ressource introuvable";
      case CONFLICT -> "Conflit";
      case FORBIDDEN -> "Opération interdite";
      case BAD_REQUEST -> "Requête invalide";
      default -> "Règle de gestion non respectée";
    };
  }
}
