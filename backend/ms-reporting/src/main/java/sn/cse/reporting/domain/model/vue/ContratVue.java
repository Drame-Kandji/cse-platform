package sn.cse.reporting.domain.model.vue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import sn.cse.reporting.domain.model.StatutContrat;

/**
 * Vue d'un contrat d'achat : montant, engagement (R06) et livraison prévue / réelle.
 *
 * @param id identifiant
 * @param numero numéro ({@code CTR-2026-001})
 * @param appelOffreId appel d'offres d'origine
 * @param chantierId chantier bénéficiaire
 * @param fournisseurId fournisseur
 * @param fournisseurNom raison sociale du fournisseur
 * @param montant montant (FCFA)
 * @param statut statut courant
 * @param dateSignature date de signature
 * @param dateEngagement date d'engagement budgétaire validé par la DAF
 * @param dateLivraisonPrevue date de livraison prévue
 * @param dateLivraisonReelle date de livraison réelle
 * @param majLe instant du dernier événement appliqué
 */
public record ContratVue(
    UUID id,
    String numero,
    UUID appelOffreId,
    UUID chantierId,
    UUID fournisseurId,
    String fournisseurNom,
    BigDecimal montant,
    StatutContrat statut,
    LocalDate dateSignature,
    LocalDate dateEngagement,
    LocalDate dateLivraisonPrevue,
    LocalDate dateLivraisonReelle,
    Instant majLe) {

  /** Statuts postérieurs à la validation de l'engagement. */
  private static final Set<StatutContrat> ENGAGES =
      EnumSet.of(
          StatutContrat.ENGAGEMENT_VALIDE,
          StatutContrat.EN_LIVRAISON,
          StatutContrat.LIVRE,
          StatutContrat.CLOTURE);

  /** Statuts d'un contrat en cours d'exécution. */
  private static final Set<StatutContrat> EN_COURS =
      EnumSet.of(
          StatutContrat.BROUILLON,
          StatutContrat.EN_ATTENTE_SIGNATURE,
          StatutContrat.SIGNE,
          StatutContrat.ENGAGEMENT_VALIDE,
          StatutContrat.EN_LIVRAISON);

  public ContratVue {
    Objects.requireNonNull(id, "id");
    montant = montant == null ? BigDecimal.ZERO : montant;
  }

  /**
   * Date d'engagement déduite d'un instantané : la date de signature (l'engagement est demandé à la
   * signature, R06), à défaut le jour de l'événement ; aucune si le statut ne traduit pas encore un
   * engagement validé.
   *
   * @param statut statut de l'instantané
   * @param dateSignature date de signature
   * @param quand instant de l'événement
   * @return la date d'engagement, ou {@code null}
   */
  public static LocalDate dateEngagementDeduite(
      StatutContrat statut, LocalDate dateSignature, Instant quand) {
    return statut != null && ENGAGES.contains(statut)
        ? Horodatage.sinon(dateSignature, Horodatage.jour(quand))
        : null;
  }

  /**
   * @param recu vue construite à partir de l'événement reçu
   * @return l'instantané le plus récent, la première date d'engagement connue étant conservée
   */
  public ContratVue fusionnerAvec(ContratVue recu) {
    ContratVue r = Horodatage.plusRecent(recu.majLe, majLe) ? recu : this;
    return new ContratVue(
        id,
        r.numero,
        r.appelOffreId,
        r.chantierId,
        r.fournisseurId,
        r.fournisseurNom,
        r.montant,
        r.statut,
        r.dateSignature,
        Horodatage.min(dateEngagement, recu.dateEngagement),
        r.dateLivraisonPrevue,
        Horodatage.sinon(
            r.dateLivraisonReelle, Horodatage.sinon(dateLivraisonReelle, recu.dateLivraisonReelle)),
        Horodatage.max(majLe, recu.majLe));
  }

  /**
   * @return vrai si l'engagement a été validé
   */
  public boolean engage() {
    return dateEngagement != null;
  }

  /**
   * @return vrai si la livraison a eu lieu
   */
  public boolean livre() {
    return dateLivraisonReelle != null;
  }

  /**
   * @return vrai si le contrat a été livré à la date prévue ou avant (non défini sans date prévue)
   */
  public boolean livreALHeure() {
    return livre()
        && dateLivraisonPrevue != null
        && !dateLivraisonReelle.isAfter(dateLivraisonPrevue);
  }

  /**
   * @return vrai si la ponctualité est mesurable (livré avec une date prévue)
   */
  public boolean ponctualiteMesurable() {
    return livre() && dateLivraisonPrevue != null;
  }

  /**
   * @param aujourdhui date du jour
   * @return vrai si la livraison prévue est dépassée sans livraison
   */
  public boolean enRetard(LocalDate aujourdhui) {
    return !livre()
        && statut != StatutContrat.CLOTURE
        && dateLivraisonPrevue != null
        && dateLivraisonPrevue.isBefore(aujourdhui);
  }

  /**
   * @return vrai si le contrat n'est ni livré ni clôturé
   */
  public boolean enCours() {
    return statut != null && EN_COURS.contains(statut);
  }
}
