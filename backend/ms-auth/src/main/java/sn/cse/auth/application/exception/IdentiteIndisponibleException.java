package sn.cse.auth.application.exception;

import sn.cse.common.exception.ServiceUnavailableException;

/**
 * Le fournisseur d'identité (Keycloak) est injoignable ou en erreur : l'opération n'a pas pu être
 * réalisée et peut être retentée (HTTP 503 {@code IDENTITE_INDISPONIBLE}). Le message exposé au
 * client est fixe ; la cause technique n'est que journalisée.
 */
public class IdentiteIndisponibleException extends ServiceUnavailableException {

  /** Message destiné à l'utilisateur. */
  public static final String MESSAGE =
      "Le service d'authentification est momentanément indisponible. Réessayez dans quelques"
          + " instants.";

  private final String raison;

  /**
   * @param raison description technique (journalisée, jamais renvoyée au client)
   * @param cause cause d'origine (peut être {@code null})
   */
  public IdentiteIndisponibleException(String raison, Throwable cause) {
    super("IDENTITE_INDISPONIBLE", MESSAGE);
    this.raison = raison;
    if (cause != null) {
      initCause(cause);
    }
  }

  /**
   * @return la raison technique de l'indisponibilité
   */
  public String raison() {
    return raison;
  }
}
