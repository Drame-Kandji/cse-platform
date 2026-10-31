package sn.cse.reporting.domain.model.vue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.StatutFacture;

/**
 * Vue d'une facture fournisseur. Mandatement et paiement sont datés par les dates métier du payload
 * ({@code dateMandatement}, {@code datePaiement}) ; à défaut (événements antérieurs à leur
 * introduction), par le jour des événements {@code FACTURE_MANDATEE} / {@code FACTURE_PAYEE}.
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
   * Dates déduites du statut d'un instantané reçu à l'instant {@code quand} : les dates métier du
   * payload priment ; à défaut, le jour de l'événement. La réception est ramenée à la date
   * d'émission si celle-ci est antérieure (factures historiques publiées a posteriori).
   *
   * @param statut statut de l'instantané
   * @param dates dates métier portées par le payload (éléments éventuellement nuls)
   * @param quand instant de l'événement
   * @return tableau {réception, mandatement, paiement} (éléments éventuellement nuls)
   */
  public static LocalDate[] datesDeduites(StatutFacture statut, DatesMetier dates, Instant quand) {
    LocalDate jour = Horodatage.jour(quand);
    LocalDate reception = Horodatage.min(jour, dates.emission());
    LocalDate mandatement =
        statut == StatutFacture.MANDATEE || statut == StatutFacture.PAYEE
            ? Horodatage.sinon(dates.mandatement(), jour)
            : null;
    LocalDate paiement =
        statut == StatutFacture.PAYEE ? Horodatage.sinon(dates.paiement(), jour) : null;
    return new LocalDate[] {reception, mandatement, paiement};
  }

  /**
   * Dates métier d'une facture portées par le payload.
   *
   * @param emission date d'émission
   * @param mandatement date de mandatement
   * @param paiement date de paiement
   */
  public record DatesMetier(LocalDate emission, LocalDate mandatement, LocalDate paiement) {

    /**
     * @return aucune date métier connue
     */
    public static DatesMetier aucune() {
      return new DatesMetier(null, null, null);
    }
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
