package sn.cse.auth.application.port.in;

import sn.cse.auth.application.query.ResultatSynchronisation;

/** Import idempotent des comptes Keycloak portant un rôle CSE dans les profils. */
public interface SynchroniserUtilisateursUseCase {

  /**
   * @return le bilan de la synchronisation
   * @throws sn.cse.auth.application.exception.IdentiteIndisponibleException si Keycloak est
   *     injoignable
   */
  ResultatSynchronisation synchroniser();
}
