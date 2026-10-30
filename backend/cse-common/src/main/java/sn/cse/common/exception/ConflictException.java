package sn.cse.common.exception;

/** Transition d'état interdite ou conflit (affectation concurrente, doublon) : HTTP 409. */
public class ConflictException extends CseException {

  public ConflictException(String code, String message) {
    super(code, message);
  }

  @Override
  public int status() {
    return 409;
  }
}
