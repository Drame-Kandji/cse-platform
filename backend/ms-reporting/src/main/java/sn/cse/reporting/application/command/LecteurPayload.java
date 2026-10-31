package sn.cse.reporting.application.command;

/**
 * Désérialisation différée du payload d'un événement : le cas d'utilisation choisit le type de
 * payload selon le type d'événement, l'adaptateur Kafka fournit la conversion.
 */
public interface LecteurPayload {

  /**
   * @param type classe du payload attendu (record de {@code sn.cse.events.payload})
   * @param <T> type du payload
   * @return le payload typé
   * @throws IllegalArgumentException si le payload ne correspond pas au type (non rejoué)
   */
  <T> T lire(Class<T> type);
}
