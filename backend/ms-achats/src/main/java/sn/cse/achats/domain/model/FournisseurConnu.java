package sn.cse.achats.domain.model;

import java.util.Objects;
import java.util.UUID;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.exception.Regles;

/**
 * Fournisseur tel que vérifié auprès de ms-fournisseurs (ou de la projection locale en repli).
 *
 * @param id identifiant
 * @param raisonSociale raison sociale
 * @param statut statut courant
 */
public record FournisseurConnu(UUID id, String raisonSociale, StatutFournisseur statut) {

  public FournisseurConnu {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(raisonSociale, "raisonSociale");
    Objects.requireNonNull(statut, "statut");
  }

  /**
   * Règle R14 : un fournisseur suspendu, blacklisté ou non validé ne peut être ni invité ni
   * soumissionner.
   *
   * @throws sn.cse.common.exception.BusinessRuleException si le fournisseur n'est pas actif
   */
  public void exigerEligible() {
    Regles.exiger(
        statut.peutParticiper(),
        CodesErreur.FOURNISSEUR_NON_ELIGIBLE,
        "Le fournisseur « "
            + raisonSociale
            + " » ("
            + statut
            + ") ne peut pas participer à une consultation (R14).");
  }

  /**
   * @return la référence d'invitation correspondante
   */
  public FournisseurInvite commeInvite() {
    return new FournisseurInvite(id, raisonSociale);
  }
}
