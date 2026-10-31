package sn.cse.achats.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Proposition d'attribution d'un marché et circuit d'approbation (règle R11) : co-validation du
 * Responsable Matériel toujours requise, approbation de la Direction Générale requise si le montant
 * dépasse le seuil de délégation.
 *
 * @param offreId offre proposée
 * @param montant montant attribué (montant de l'offre)
 * @param justification justification du Responsable des Achats
 * @param proposeePar auteur de la proposition
 * @param dateProposition instant de la proposition
 * @param coValidationMateriel co-validation du Responsable Matériel
 * @param approbationDg approbation DG, {@code null} si non requise
 */
public record Attribution(
    UUID offreId,
    BigDecimal montant,
    String justification,
    Acteur proposeePar,
    Instant dateProposition,
    Approbation coValidationMateriel,
    Approbation approbationDg) {

  public Attribution {
    Objects.requireNonNull(offreId, "offreId");
    Objects.requireNonNull(montant, "montant");
    Objects.requireNonNull(justification, "justification");
    Objects.requireNonNull(proposeePar, "proposeePar");
    Objects.requireNonNull(dateProposition, "dateProposition");
    Objects.requireNonNull(coValidationMateriel, "coValidationMateriel");
  }

  /**
   * @return vrai si l'approbation de la Direction Générale est requise (R11)
   */
  public boolean approbationDgRequise() {
    return approbationDg != null;
  }

  /**
   * @return vrai si toutes les approbations requises sont acquises
   */
  public boolean estCompletementApprouvee() {
    return coValidationMateriel.estApprouvee()
        && (approbationDg == null || approbationDg.estApprouvee());
  }

  Attribution avecCoValidation(Approbation decision) {
    return new Attribution(
        offreId, montant, justification, proposeePar, dateProposition, decision, approbationDg);
  }

  Attribution avecApprobationDg(Approbation decision) {
    return new Attribution(
        offreId,
        montant,
        justification,
        proposeePar,
        dateProposition,
        coValidationMateriel,
        decision);
  }
}
