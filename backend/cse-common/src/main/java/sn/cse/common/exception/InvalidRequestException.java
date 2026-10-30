package sn.cse.common.exception;

/**
 * Paramètre de requête invalide non couvert par Jakarta Validation (ex. tri non autorisé) : HTTP
 * 400.
 */
public class InvalidRequestException extends CseException {

  public InvalidRequestException(String code, String message) {
    super(code, message);
  }

  @Override
  public int status() {
    return 400;
  }
}
