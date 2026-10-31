package sn.cse.besoins.domain.exception;

import sn.cse.common.exception.ForbiddenOperationException;

/** Règle R01 : seul le détenteur du rôle de l'étape courante valide ou rejette (HTTP 403). */
public class ValidationNonAutoriseeException extends ForbiddenOperationException {

  /**
   * @param etape étape de validation en attente
   * @param roleRequis rôle exigé
   */
  public ValidationNonAutoriseeException(String etape, String roleRequis) {
    super(
        CodesErreurBesoin.VALIDATION_NON_AUTORISEE,
        "L'étape de validation « "
            + etape
            + " » est réservée au rôle "
            + roleRequis
            + " (règle R01).");
  }
}
