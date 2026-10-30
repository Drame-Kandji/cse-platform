package sn.cse.auth.application.port.out;

/**
 * Enregistre une action de compensation exécutée si la transaction en cours est annulée : un compte
 * créé dans Keycloak ne doit pas survivre à l'échec de l'enregistrement du profil.
 */
public interface CompensationTransactionnelle {

  /**
   * @param compensation action à exécuter en cas d'annulation
   */
  void siAnnulation(Runnable compensation);
}
