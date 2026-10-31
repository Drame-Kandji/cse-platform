package sn.cse.reporting.domain.model.vue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.StatutFacture;

/**
 * Vue d'une facture fournisseur. Le payload ne porte pas les dates de mandatement et de paiement :
 * elles sont prises au jour des événements {@code FACTURE_MANDATEE} / {@code FACTURE_PAYEE}.
 *
 * @param id identifiant
 * @param numero numéro
 * @param contratId contrat facturé
 * @param fournisseurId fournisseur
 * @param montantTtc montant TTC (FCFA)
 * @param statut statut courant
 * @param dateEcheance échéance
 * @param dateReception jour de réception
 * @param dateMandatement jour de mandatement
 * @param datePaiement jour de paiement
 * @param majLe instant du dernier événement appliqué
 */
public record FactureVue(
    UUID id,
    String numero,
    UUID contratId,
    UUID fournisseurId,
    BigDecimal montantTtc,
    StatutFacture statut,
    LocalDate dateEcheance,
    LocalDate dateReception,
    LocalDate dateMandatement,
    LocalDate datePaiement,
    Instant majLe) {

  public FactureVue {
    Objects.requireNonNull(id, "id");
    montantTtc = montantTtc == null ? BigDecimal.ZERO : montantTtc;
  }

  /**
   * Dates déduites du statut d'un instantané reçu à l'instant {@code quand}.
   *
   * @param statut statut de l'instantané
   * @param quand instant de l'événement
   * @return tableau {réception, mandatement, paiement} (éléments éventuellement nuls)
   */
  public static LocalDate[] datesDeduites(StatutFacture statut, Instant quand) {
    LocalDate jour = Horodatage.jour(quand);
    LocalDate reception = jour;
    LocalDate mandatement =
        statut == StatutFacture.MANDATEE || statut == StatutFacture.PAYEE ? jour : null;
    LocalDate paiement = statut == StatutFacture.PAYEE ? jour : null;
    return new LocalDate[] {reception, mandatement, paiement};
  }

  /**
   * @param recu vue construite à partir de l'événement reçu
   * @return l'instantané le plus récent, chaque date conservant sa première valeur connue
   */
  public FactureVue fusionnerAvec(FactureVue recu) {
    FactureVue r = Horodatage.plusRecent(recu.majLe, majLe) ? recu : this;
    return new FactureVue(
        id,
        r.numero,
        r.contratId,
        r.fournisseurId,
        r.montantTtc,
        r.statut,
        r.dateEcheance,
        Horodatage.min(dateReception, recu.dateReception),
        Horodatage.min(dateMandatement, recu.dateMandatement),
        Horodatage.min(datePaiement, recu.datePaiement),
        Horodatage.max(majLe, recu.majLe));
  }

  /**
   * @param periode période
   * @return vrai si la facture a été mandatée dans la période (et n'est pas rejetée)
   */
  public boolean mandateeDans(Periode periode) {
    return statut != StatutFacture.REJETEE && periode.contient(dateMandatement);
  }

  /**
   * @param periode période
   * @return vrai si la facture a été payée dans la période
   */
  public boolean payeeDans(Periode periode) {
    return statut == StatutFacture.PAYEE && periode.contient(datePaiement);
  }
}
