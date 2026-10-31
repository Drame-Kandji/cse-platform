package sn.cse.achats.application.service;

import sn.cse.achats.domain.model.Acteur;
import sn.cse.common.security.CurrentUser;

/** Conversion de l'utilisateur authentifié en acteur du domaine. */
final class Acteurs {

  private static final String ANONYME = "Utilisateur";

  private Acteurs() {}

  static Acteur de(CurrentUser utilisateur) {
    if (utilisateur == null) {
      return Acteur.SYSTEME;
    }
    String nom =
        utilisateur.nom() == null || utilisateur.nom().isBlank() ? ANONYME : utilisateur.nom();
    return new Acteur(utilisateur.id(), nom);
  }
}
