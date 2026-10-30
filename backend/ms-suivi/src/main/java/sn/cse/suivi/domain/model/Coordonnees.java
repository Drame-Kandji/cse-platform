package sn.cse.suivi.domain.model;

import sn.cse.suivi.domain.exception.SuiviErreurs;

/**
 * Position GPS (WGS 84).
 *
 * @param latitude latitude en degrés décimaux [-90, 90]
 * @param longitude longitude en degrés décimaux [-180, 180]
 */
public record Coordonnees(double latitude, double longitude) {

  public Coordonnees {
    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
      throw SuiviErreurs.regle(
          "COORDONNEES_INVALIDES",
          "Coordonnées GPS hors limites : (" + latitude + ", " + longitude + ").");
    }
  }

  /**
   * @param latitude latitude ou {@code null}
   * @param longitude longitude ou {@code null}
   * @return les coordonnées, ou {@code null} si l'une des deux manque
   */
  public static Coordonnees depuis(Double latitude, Double longitude) {
    return latitude == null || longitude == null ? null : new Coordonnees(latitude, longitude);
  }
}
