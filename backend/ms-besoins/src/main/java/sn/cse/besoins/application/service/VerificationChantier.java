package sn.cse.besoins.application.service;

import java.util.UUID;
import org.springframework.stereotype.Component;
import sn.cse.besoins.application.port.out.ChantierPort;
import sn.cse.besoins.application.port.out.ChantierPort.ChantierInfo;
import sn.cse.common.exception.BusinessRuleException;

/**
 * Vérifie auprès de ms-suivi qu'un chantier existe et est en cours avant d'y rattacher un besoin.
 */
@Component
class VerificationChantier {

  static final String CHANTIER_INTROUVABLE = "CHANTIER_INTROUVABLE";
  static final String CHANTIER_INACTIF = "CHANTIER_INACTIF";

  private final ChantierPort chantiers;

  VerificationChantier(ChantierPort chantiers) {
    this.chantiers = chantiers;
  }

  ChantierInfo exigerActif(UUID chantierId) {
    ChantierInfo chantier =
        chantiers
            .trouver(chantierId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        CHANTIER_INTROUVABLE, "Le chantier " + chantierId + " n'existe pas."));
    if (!chantier.actif()) {
      throw new BusinessRuleException(
          CHANTIER_INACTIF,
          "Le chantier « "
              + chantier.nom()
              + " » est terminé : aucun besoin ne peut y être rattaché.");
    }
    return chantier;
  }
}
