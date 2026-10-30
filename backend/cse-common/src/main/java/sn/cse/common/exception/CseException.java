package sn.cse.common.exception;

import java.util.Objects;

/**
 * Exception métier de base, indépendante de tout framework afin d'être levée depuis le domaine.
 * Chaque sous-classe correspond à une famille de statut HTTP traduite en ProblemDetail (RFC 7807).
 */
public abstract class CseException extends RuntimeException {

  private final String code;

  protected CseException(String code, String message) {
    super(message);
    this.code = Objects.requireNonNull(code, "code");
  }

  /**
   * @return code d'erreur stable, exposé au client (ex. {@code BESOIN_NON_MODIFIABLE})
   */
  public String getCode() {
    return code;
  }

  /**
   * @return statut HTTP correspondant
   */
  public abstract int status();
}
