package sn.cse.achats.domain.model;

import java.math.BigDecimal;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.exception.Regles;

/**
 * Clauses particulières d'un contrat d'achat.
 *
 * @param garantieMois garantie en mois
 * @param conditionsMaintenance conditions de maintenance
 * @param conditionsLivraison conditions de livraison
 * @param penaliteRetardPctParJour pénalité de retard en % du montant par jour
 */
public record ClausesContrat(
    Integer garantieMois,
    String conditionsMaintenance,
    String conditionsLivraison,
    BigDecimal penaliteRetardPctParJour) {

  public ClausesContrat {
    Regles.exiger(
        garantieMois == null || garantieMois >= 0,
        CodesErreur.DONNEE_INVALIDE,
        "La garantie ne peut pas être négative.");
    Regles.exiger(
        penaliteRetardPctParJour == null
            || (penaliteRetardPctParJour.signum() >= 0
                && penaliteRetardPctParJour.compareTo(BigDecimal.valueOf(100)) <= 0),
        CodesErreur.DONNEE_INVALIDE,
        "La pénalité de retard doit être comprise entre 0 et 100 % par jour.");
    conditionsMaintenance = Regles.facultatif(conditionsMaintenance);
    conditionsLivraison = Regles.facultatif(conditionsLivraison);
  }
}
