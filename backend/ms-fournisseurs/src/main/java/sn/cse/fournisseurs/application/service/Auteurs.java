package sn.cse.fournisseurs.application.service;

import sn.cse.common.security.CurrentUser;
import sn.cse.fournisseurs.domain.model.Auteur;

/** Conversion de l'utilisateur authentifié en auteur du domaine. */
final class Auteurs {

  private Auteurs() {}

  static Auteur depuis(CurrentUser utilisateur) {
    if (utilisateur == null) {
      return Auteur.SYSTEME;
    }
    String nom =
        utilisateur.nom() == null || utilisateur.nom().isBlank()
            ? "Utilisateur"
            : utilisateur.nom();
    return new Auteur(utilisateur.id(), nom);
  }
}
