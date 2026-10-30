package sn.cse.auth.domain.exception;

import sn.cse.common.exception.ConflictException;

/** Adresse e-mail déjà associée à un compte : HTTP 409 {@code EMAIL_DEJA_UTILISE}. */
public class EmailDejaUtiliseException extends ConflictException {

  /**
   * @param email adresse en conflit
   */
  public EmailDejaUtiliseException(String email) {
    super("EMAIL_DEJA_UTILISE", "Un compte existe déjà avec l'adresse « " + email + " ».");
  }
}
