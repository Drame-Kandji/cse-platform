package sn.cse.notification.application.port.in;

import sn.cse.notification.domain.model.UtilisateurAnnuaire;

/** Met à jour l'annuaire local à partir des événements de ms-auth. */
public interface SynchroniserAnnuaireUseCase {

  /**
   * @param utilisateur nouvelle version de l'utilisateur
   * @return vrai si l'annuaire a été modifié (faux si la version reçue est plus ancienne)
   */
  boolean synchroniser(UtilisateurAnnuaire utilisateur);
}
