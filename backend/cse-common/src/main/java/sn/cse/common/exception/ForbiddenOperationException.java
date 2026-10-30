package sn.cse.common.exception;

/**
 * Opération interdite pour l'utilisateur courant malgré un rôle valide (ex. chantier non attribué)
 * : HTTP 403.
 */
public class ForbiddenOperationException extends CseException {

  public ForbiddenOperationException(String code, String message) {
    super(code, message);
  }

  @Override
  public int status() {
    return 403;
  }
}
