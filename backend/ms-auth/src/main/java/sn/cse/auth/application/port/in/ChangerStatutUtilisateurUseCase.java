package sn.cse.auth.application.port.in;

import sn.cse.auth.application.command.ChangerStatutCommand;
import sn.cse.auth.domain.model.Utilisateur;

/** Activation / désactivation d'un compte. */
public interface ChangerStatutUtilisateurUseCase {

  /**
   * @param commande statut cible
   * @return le profil modifié
   */
  Utilisateur changerStatut(ChangerStatutCommand commande);
}
