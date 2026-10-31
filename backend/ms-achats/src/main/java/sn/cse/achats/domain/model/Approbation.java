package sn.cse.achats.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.common.exception.ConflictException;

/**
 * Décision d'un approbateur (co-validation RM, approbation DG).
 *
 * @param statut statut de la décision
 * @param parId identifiant du décideur
 * @param parNom nom du décideur
 * @param date instant de la décision
 * @param commentaire commentaire facultatif
 */
public record Approbation(
    StatutApprobation statut, UUID parId, String parNom, Instant date, String commentaire) {

  public Approbation {
    Objects.requireNonNull(statut, "statut");
  }

  /**
   * @return une approbation en attente
   */
  public static Approbation enAttente() {
    return new Approbation(StatutApprobation.EN_ATTENTE, null, null, null, null);
  }

  /**
   * @param approuve décision
   * @param acteur décideur
   * @param date instant
   * @param commentaire commentaire facultatif
   * @return la décision prise
   * @throws ConflictException si la décision a déjà été prise
   */
  public Approbation decider(boolean approuve, Acteur acteur, Instant date, String commentaire) {
    if (statut != StatutApprobation.EN_ATTENTE) {
      throw new ConflictException(
          CodesErreur.DECISION_DEJA_PRISE, "Cette décision a déjà été prise (" + statut + ").");
    }
    return new Approbation(
        approuve ? StatutApprobation.APPROUVEE : StatutApprobation.REFUSEE,
        acteur.id(),
        acteur.nom(),
        date,
        commentaire);
  }

  /**
   * @return vrai si approuvée
   */
  public boolean estApprouvee() {
    return statut == StatutApprobation.APPROUVEE;
  }
}
