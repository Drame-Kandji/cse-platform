package sn.cse.besoins.infrastructure.client;

/** Réponse 5xx de ms-suivi : erreur transitoire, rejouée par Resilience4j. */
public class SuiviErreurTechniqueException extends RuntimeException {

  /**
   * @param message description (statut HTTP et opération)
   */
  public SuiviErreurTechniqueException(String message) {
    super(message);
  }
}
