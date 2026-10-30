package sn.cse.auth.application.port.in;

import sn.cse.auth.application.command.ModifierUtilisateurCommand;
import sn.cse.auth.domain.model.Utilisateur;

/** Modification d'un compte (identité, rôle, attributs). */
public interface ModifierUtilisateurUseCase {

  /**
   * @param commande modifications
   * @return le profil modifié
   */
  Utilisateur modifier(ModifierUtilisateurCommand commande);
}
