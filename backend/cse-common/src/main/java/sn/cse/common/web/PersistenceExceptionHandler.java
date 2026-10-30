package sn.cse.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erreurs de persistance : verrou optimiste et contrainte d'unicité traduits en 409. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PersistenceExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(PersistenceExceptionHandler.class);

  /**
   * @return 409 : la ressource a été modifiée entre-temps
   */
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> concurrence() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ProblemDetails.of(
                HttpStatus.CONFLICT,
                "CONFLIT_CONCURRENCE",
                "Modification concurrente",
                "Cette donnée a été modifiée par un autre utilisateur. Rechargez puis réessayez."));
  }

  /**
   * @param ex violation de contrainte en base
   * @return 409 générique (le détail SQL n'est jamais exposé)
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> integrite(DataIntegrityViolationException ex) {
    LOG.warn("Violation d'intégrité : {}", ex.getMostSpecificCause().getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ProblemDetails.of(
                HttpStatus.CONFLICT,
                "CONFLIT_DONNEES",
                "Conflit de données",
                "Cette opération entre en conflit avec une donnée existante (doublon ?)."));
  }
}
