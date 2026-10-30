package sn.cse.besoins.application.service;

import java.util.UUID;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.security.CurrentUser;

/** Contrôle d'accès « données » : un chef de chantier n'agit que sur ses chantiers. */
final class ControleAccesBesoin {

  static final String ACCES_CHANTIER_REFUSE = "ACCES_CHANTIER_REFUSE";
  static final String CHANTIER_NON_ATTRIBUE = "CHANTIER_NON_ATTRIBUE";

  private ControleAccesBesoin() {}

  static void verifierLecture(BesoinMateriel besoin, CurrentUser utilisateur) {
    if (!utilisateur.peutAccederAuChantier(besoin.chantierId())) {
      throw new ForbiddenOperationException(
          ACCES_CHANTIER_REFUSE,
          "Le besoin "
              + besoin.reference()
              + " concerne un chantier qui ne vous est pas attribué.");
    }
  }

  static void verifierChantierAttribue(UUID chantierId, CurrentUser utilisateur) {
    if (!utilisateur.peutAccederAuChantier(chantierId)) {
      throw new ForbiddenOperationException(
          CHANTIER_NON_ATTRIBUE, "Ce chantier ne vous est pas attribué.");
    }
  }
}
