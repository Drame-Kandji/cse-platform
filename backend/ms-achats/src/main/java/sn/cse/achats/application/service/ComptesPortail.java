package sn.cse.achats.application.service;

import java.util.UUID;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.security.CurrentUser;

/** Rattachement d'un compte du portail à son fournisseur (claim {@code fournisseur_id}). */
final class ComptesPortail {

  private ComptesPortail() {}

  /**
   * @param utilisateur utilisateur du portail
   * @return le fournisseur rattaché au jeton
   * @throws ForbiddenOperationException COMPTE_NON_RATTACHE si le claim est absent
   */
  static UUID fournisseurDe(CurrentUser utilisateur) {
    return utilisateur
        .fournisseur()
        .orElseThrow(
            () ->
                new ForbiddenOperationException(
                    CodesErreur.COMPTE_NON_RATTACHE,
                    "Ce compte n'est rattaché à aucun fournisseur."));
  }
}
