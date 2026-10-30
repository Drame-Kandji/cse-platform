package sn.cse.besoins.application.exception;

import sn.cse.common.exception.CseException;

/**
 * Le service ms-suivi ne répond pas (panne, délai dépassé, circuit ouvert) : HTTP 503 lorsqu'elle
 * remonte jusqu'au client. Lors de l'orientation, elle est absorbée : le besoin reste {@code
 * VALIDE} avec {@code orientationEnAttente=true}.
 */
public class ServiceSuiviIndisponibleException extends CseException {

  /** Code d'erreur exposé. */
  public static final String CODE = "SERVICE_SUIVI_INDISPONIBLE";

  /**
   * @param operation opération tentée (pour le journal)
   */
  public ServiceSuiviIndisponibleException(String operation) {
    super(
        CODE,
        "Le service de suivi du parc est momentanément indisponible ("
            + operation
            + "). Réessayez dans quelques instants.");
  }

  @Override
  public int status() {
    return 503;
  }
}
