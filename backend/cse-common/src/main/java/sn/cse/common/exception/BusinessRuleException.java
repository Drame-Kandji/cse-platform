package sn.cse.common.exception;

/** Violation d'une règle de gestion (ex. R02, R04, R08) : HTTP 422. */
public class BusinessRuleException extends CseException {

  public BusinessRuleException(String code, String message) {
    super(code, message);
  }

  @Override
  public int status() {
    return 422;
  }
}
