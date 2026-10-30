package sn.cse.common.exception;

/** Service dépendant indisponible (circuit ouvert, délai dépassé) : HTTP 503. */
public class ServiceUnavailableException extends CseException {

  public ServiceUnavailableException(String code, String message) {
    super(code, message);
  }

  @Override
  public int status() {
    return 503;
  }
}
