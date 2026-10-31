package sn.cse.achats.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.common.exception.ConflictException;

/**
 * Engagement financier demandé à la DAF lors de la signature d'un contrat (R06).
 *
 * @param statut décision de la DAF
 * @param montant montant engagé (FCFA)
 * @param parId décideur
 * @param parNom nom du décideur
 * @param dateDemande instant de la demande
 * @param dateDecision instant de la décision
 * @param commentaire commentaire de la DAF
 */
public record Engagement(
    StatutApprobation statut,
    BigDecimal montant,
    UUID parId,
    String parNom,
    Instant dateDemande,
    Instant dateDecision,
    String commentaire) {

  public Engagement {
    Objects.requireNonNull(statut, "statut");
    Objects.requireNonNull(montant, "montant");
    Objects.requireNonNull(dateDemande, "dateDemande");
  }

  /**
   * @param montant montant du contrat
   * @param maintenant instant de la demande
   * @return un engagement en attente de la DAF
   */
  public static Engagement demander(BigDecimal montant, Instant maintenant) {
    return new Engagement(
        StatutApprobation.EN_ATTENTE, montant, null, null, maintenant, null, null);
  }

  /**
   * @param approuve décision
   * @param daf décideur
   * @param maintenant instant
   * @param commentaireDecision commentaire
   * @return l'engagement décidé
   * @throws ConflictException si l'engagement a déjà été traité
   */
  public Engagement decider(
      boolean approuve, Acteur daf, Instant maintenant, String commentaireDecision) {
    if (statut != StatutApprobation.EN_ATTENTE) {
      throw new ConflictException(
          CodesErreur.ENGAGEMENT_NON_EN_ATTENTE,
          "L'engagement a déjà été traité (" + statut + ").");
    }
    return new Engagement(
        approuve ? StatutApprobation.APPROUVEE : StatutApprobation.REFUSEE,
        montant,
        daf.id(),
        daf.nom(),
        dateDemande,
        maintenant,
        commentaireDecision);
  }
}
