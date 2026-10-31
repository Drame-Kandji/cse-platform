package sn.cse.achats.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.exception.Regles;

/**
 * Agrégat facture fournisseur rattachée à un contrat. Cycle de vie : RECUE → MANDATEE → PAYEE, ou
 * RECUE → REJETEE. Une facture non payée dont l'échéance est dépassée est en retard.
 */
public final class Facture {

  private final UUID id;
  private final String numero;
  private final UUID contratId;
  private final String contratNumero;
  private final UUID fournisseurId;
  private final String fournisseurNom;
  private final BigDecimal montantHt;
  private final BigDecimal tva;
  private final LocalDate dateEmission;
  private final LocalDate dateEcheance;
  private StatutFacture statut;
  private String referenceMandat;
  private LocalDate dateMandatement;
  private LocalDate datePaiement;
  private String motifRejet;
  private final long version;

  /**
   * Reconstitution depuis la persistance.
   *
   * @param id identifiant
   * @param numero numéro de facture du fournisseur
   * @param contratId contrat
   * @param contratNumero numéro du contrat
   * @param fournisseurId fournisseur émetteur
   * @param fournisseurNom raison sociale
   * @param montantHt montant hors taxes
   * @param tva montant de TVA
   * @param dateEmission date d'émission
   * @param dateEcheance date d'échéance
   * @param statut statut
   * @param referenceMandat référence du mandat de paiement
   * @param dateMandatement date de mandatement
   * @param datePaiement date de paiement
   * @param motifRejet motif de rejet
   * @param version version de verrou optimiste
   */
  public Facture(
      UUID id,
      String numero,
      UUID contratId,
      String contratNumero,
      UUID fournisseurId,
      String fournisseurNom,
      BigDecimal montantHt,
      BigDecimal tva,
      LocalDate dateEmission,
      LocalDate dateEcheance,
      StatutFacture statut,
      String referenceMandat,
      LocalDate dateMandatement,
      LocalDate datePaiement,
      String motifRejet,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.numero = Objects.requireNonNull(numero, "numero");
    this.contratId = Objects.requireNonNull(contratId, "contratId");
    this.contratNumero = contratNumero;
    this.fournisseurId = Objects.requireNonNull(fournisseurId, "fournisseurId");
    this.fournisseurNom = fournisseurNom;
    this.montantHt = Objects.requireNonNull(montantHt, "montantHt");
    this.tva = Objects.requireNonNull(tva, "tva");
    this.dateEmission = Objects.requireNonNull(dateEmission, "dateEmission");
    this.dateEcheance = Objects.requireNonNull(dateEcheance, "dateEcheance");
    this.statut = Objects.requireNonNull(statut, "statut");
    this.referenceMandat = referenceMandat;
    this.dateMandatement = dateMandatement;
    this.datePaiement = datePaiement;
    this.motifRejet = motifRejet;
    this.version = version;
  }

  /**
   * Enregistre la réception d'une facture.
   *
   * @param contrat contrat facturé
   * @param numero numéro de facture (fourni par l'utilisateur)
   * @param montantHt montant hors taxes
   * @param tva montant de TVA
   * @param dateEmission date d'émission
   * @param dateEcheance date d'échéance
   * @param dejaFactureTtc cumul TTC des factures non rejetées du contrat
   * @return la facture au statut RECUE
   * @throws sn.cse.common.exception.BusinessRuleException contrat non facturable, montants ou dates
   *     invalides, dépassement du montant du contrat
   */
  public static Facture recevoir(
      ContratAchat contrat,
      String numero,
      BigDecimal montantHt,
      BigDecimal tva,
      LocalDate dateEmission,
      LocalDate dateEcheance,
      BigDecimal dejaFactureTtc) {
    Regles.exiger(
        contrat.getStatut().estFacturable(),
        CodesErreur.CONTRAT_NON_FACTURABLE,
        "Le contrat "
            + contrat.getNumero()
            + " n'est pas facturable au statut "
            + contrat.getStatut()
            + " (engagement validé requis).");
    Regles.montantPositif(montantHt, "montantHt");
    Regles.exiger(
        tva != null && tva.signum() >= 0,
        CodesErreur.DONNEE_INVALIDE,
        "La TVA ne peut pas être négative.");
    Regles.exiger(
        dateEmission != null && dateEcheance != null && !dateEcheance.isBefore(dateEmission),
        CodesErreur.ECHEANCE_INVALIDE,
        "La date d'échéance ne peut pas précéder la date d'émission.");
    BigDecimal ttc = montantHt.add(tva);
    Regles.exiger(
        dejaFactureTtc.add(ttc).compareTo(contrat.getMontant()) <= 0,
        CodesErreur.DEPASSEMENT_MONTANT_CONTRAT,
        "Le cumul facturé dépasserait le montant du contrat " + contrat.getNumero() + ".");
    return new Facture(
        UUID.randomUUID(),
        Regles.texteObligatoire(numero, "numero"),
        contrat.getId(),
        contrat.getNumero(),
        contrat.getFournisseurId(),
        contrat.getFournisseurNom(),
        montantHt,
        tva,
        dateEmission,
        dateEcheance,
        StatutFacture.RECUE,
        null,
        null,
        null,
        null,
        0);
  }

  /**
   * @param reference référence du mandat
   * @param aujourdhui date de mandatement
   */
  public void mandater(String reference, LocalDate aujourdhui) {
    statut.exigerTransition(StatutFacture.MANDATEE);
    referenceMandat = Regles.texteObligatoire(reference, "referenceMandat");
    dateMandatement = aujourdhui;
    statut = StatutFacture.MANDATEE;
  }

  /**
   * @param aujourdhui date de paiement
   */
  public void payer(LocalDate aujourdhui) {
    statut.exigerTransition(StatutFacture.PAYEE);
    datePaiement = aujourdhui;
    statut = StatutFacture.PAYEE;
  }

  /**
   * @param motif motif obligatoire
   */
  public void rejeter(String motif) {
    statut.exigerTransition(StatutFacture.REJETEE);
    Regles.exiger(
        motif != null && !motif.isBlank(),
        CodesErreur.MOTIF_OBLIGATOIRE,
        "Le motif de rejet est obligatoire.");
    motifRejet = motif.trim();
    statut = StatutFacture.REJETEE;
  }

  /**
   * @param aujourdhui date du jour
   * @return vrai si la facture reste à payer après son échéance
   */
  public boolean estEnRetard(LocalDate aujourdhui) {
    return statut.estOuverte() && aujourdhui.isAfter(dateEcheance);
  }

  /**
   * @return montant toutes taxes comprises
   */
  public BigDecimal getMontantTtc() {
    return montantHt.add(tva);
  }

  public UUID getId() {
    return id;
  }

  public String getNumero() {
    return numero;
  }

  public UUID getContratId() {
    return contratId;
  }

  public String getContratNumero() {
    return contratNumero;
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public String getFournisseurNom() {
    return fournisseurNom;
  }

  public BigDecimal getMontantHt() {
    return montantHt;
  }

  public BigDecimal getTva() {
    return tva;
  }

  public LocalDate getDateEmission() {
    return dateEmission;
  }

  public LocalDate getDateEcheance() {
    return dateEcheance;
  }

  public StatutFacture getStatut() {
    return statut;
  }

  public String getReferenceMandat() {
    return referenceMandat;
  }

  public LocalDate getDateMandatement() {
    return dateMandatement;
  }

  public LocalDate getDatePaiement() {
    return datePaiement;
  }

  public String getMotifRejet() {
    return motifRejet;
  }

  public long getVersion() {
    return version;
  }
}
