package sn.cse.auth.application.port.out;

import sn.cse.auth.domain.model.Utilisateur;

/**
 * Publication des événements {@code UTILISATEUR_*} (outbox transactionnelle, topic {@code
 * cse.auth.events}). À appeler dans la transaction du cas d'utilisation.
 */
public interface UtilisateurEventPublisher {

  /**
   * @param utilisateur profil créé par un administrateur
   */
  void publierCree(Utilisateur utilisateur);

  /**
   * @param utilisateur profil modifié par un administrateur
   */
  void publierModifie(Utilisateur utilisateur);

  /**
   * @param utilisateur profil importé ou mis à jour depuis Keycloak
   */
  void publierSynchronise(Utilisateur utilisateur);
}
