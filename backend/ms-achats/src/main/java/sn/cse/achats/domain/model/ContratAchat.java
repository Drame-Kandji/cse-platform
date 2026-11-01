package sn.cse.achats.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.exception.Regles;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.ResourceNotFoundException;

/**
 * Agrégat contrat d'achat, né d'un appel d'offres attribué (R03). Cycle de vie : BROUILLON →
 * EN_ATTENTE_SIGNATURE → SIGNE → ENGAGEMENT_VALIDE → EN_LIVRAISON → LIVRE → CLOTURE (table de
 * transitions de {@link StatutContrat}). La signature se fait en deux temps (CSE puis fournisseur
 * sur le portail) ; la livraison ne démarre qu'après validation de l'engagement par la DAF (R06),
 * normalement sur déclaration d'expédition du fournisseur.
 */
public final class ContratAchat {

  private static final BigDecimal CENT = BigDecimal.valueOf(100);

  private final UUID id;
  private final String numero;
  private final UUID appelOffreId;
  private final String appelOffreReference;
  private final UUID offreId;
  private final UUID fournisseurId;
  private final String fournisseurNom;
  private final UUID besoinId;
  private final UUID chantierId;
  private final String chantierNom;
  private final String objet;
  private final String designation;
  private final CategorieMateriel categorie;
  private final int quantite;
  private final BigDecimal montant;
  private StatutContrat statut;
  private LocalDate dateSignature;
  private int delaiLivraisonJours;
  private LocalDate dateLivraisonPrevue;
  private LocalDate dateLivraisonReelle;
  private String numeroBonLivraison;
  private String observationsLivraison;
  private ClausesContrat clauses;
  private Engagement engagement;
  private final List<Jalon> jalons;
  private BigDecimal penalitesRetard;
  private LocalDate dateDerniereAlerteRetard;
  private Signature signatureCse;
  private Signature signatureFournisseur;
  private String motifRefusSignature;
  private LocalDate dateExpedition;
  private String commentaireExpedition;
  private final long version;

  /**
   * Reconstitution depuis la persistance.
   *
   * @param id identifiant
   * @param numero numéro lisible (CTR-2026-001)
   * @param appelOffreId appel d'offres d'origine
   * @param appelOffreReference référence de l'appel d'offres
   * @param offreId offre retenue
   * @param fournisseurId titulaire
   * @param fournisseurNom raison sociale du titulaire
   * @param besoinId besoin d'origine
   * @param chantierId chantier destinataire
   * @param chantierNom nom du chantier
   * @param objet objet
   * @param designation désignation du matériel
   * @param categorie catégorie
   * @param quantite quantité
   * @param montant montant (FCFA)
   * @param statut statut
   * @param dateSignature date de signature
   * @param delaiLivraisonJours délai contractuel
   * @param dateLivraisonPrevue échéance de livraison
   * @param dateLivraisonReelle date de livraison effective
   * @param numeroBonLivraison numéro du bon de livraison
   * @param observationsLivraison observations à la réception
   * @param clauses clauses particulières
   * @param engagement engagement financier
   * @param jalons jalons
   * @param penalitesRetard pénalités de retard calculées
   * @param dateDerniereAlerteRetard date de la dernière alerte de retard
   * @param signatureCse signature du CSE (responsable des achats)
   * @param signatureFournisseur signature du fournisseur (portail)
   * @param motifRefusSignature motif du dernier refus de signature du fournisseur
   * @param dateExpedition date d'expédition déclarée par le fournisseur
   * @param commentaireExpedition commentaire du fournisseur sur l'expédition
   * @param version version de verrou optimiste
   */
  public ContratAchat(
      UUID id,
      String numero,
      UUID appelOffreId,
      String appelOffreReference,
      UUID offreId,
      UUID fournisseurId,
      String fournisseurNom,
      UUID besoinId,
      UUID chantierId,
      String chantierNom,
      String objet,
      String designation,
      CategorieMateriel categorie,
      int quantite,
      BigDecimal montant,
      StatutContrat statut,
      LocalDate dateSignature,
      int delaiLivraisonJours,
      LocalDate dateLivraisonPrevue,
      LocalDate dateLivraisonReelle,
      String numeroBonLivraison,
      String observationsLivraison,
      ClausesContrat clauses,
      Engagement engagement,
      List<Jalon> jalons,
      BigDecimal penalitesRetard,
      LocalDate dateDerniereAlerteRetard,
      Signature signatureCse,
      Signature signatureFournisseur,
      String motifRefusSignature,
      LocalDate dateExpedition,
      String commentaireExpedition,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.numero = Objects.requireNonNull(numero, "numero");
    this.appelOffreId = Objects.requireNonNull(appelOffreId, "appelOffreId");
    this.appelOffreReference = appelOffreReference;
    this.offreId = Objects.requireNonNull(offreId, "offreId");
    this.fournisseurId = Objects.requireNonNull(fournisseurId, "fournisseurId");
    this.fournisseurNom = fournisseurNom;
    this.besoinId = besoinId;
    this.chantierId = chantierId;
    this.chantierNom = chantierNom;
    this.objet = objet;
    this.designation = designation;
    this.categorie = Objects.requireNonNull(categorie, "categorie");
    this.quantite = quantite;
    this.montant = Objects.requireNonNull(montant, "montant");
    this.statut = Objects.requireNonNull(statut, "statut");
    this.dateSignature = dateSignature;
    this.delaiLivraisonJours = delaiLivraisonJours;
    this.dateLivraisonPrevue = dateLivraisonPrevue;
    this.dateLivraisonReelle = dateLivraisonReelle;
    this.numeroBonLivraison = numeroBonLivraison;
    this.observationsLivraison = observationsLivraison;
    this.clauses = Objects.requireNonNull(clauses, "clauses");
    this.engagement = engagement;
    this.jalons = new ArrayList<>(jalons == null ? List.of() : jalons);
    this.penalitesRetard = penalitesRetard == null ? BigDecimal.ZERO : penalitesRetard;
    this.dateDerniereAlerteRetard = dateDerniereAlerteRetard;
    this.signatureCse = signatureCse;
    this.signatureFournisseur = signatureFournisseur;
    this.motifRefusSignature = motifRefusSignature;
    this.dateExpedition = dateExpedition;
    this.commentaireExpedition = commentaireExpedition;
    this.version = version;
  }

  /**
   * Génère le contrat BROUILLON d'un marché attribué.
   *
   * @param appelOffre appel d'offres
   * @param numero numéro attribué
   * @param parametres paramètres configurables (pénalité par défaut)
   * @return le contrat au statut BROUILLON
   * @throws sn.cse.common.exception.BusinessRuleException R03 si l'appel d'offres n'est pas
   *     attribué
   */
  public static ContratAchat generer(
      AppelOffre appelOffre, String numero, ParametresContrat parametres) {
    return generer(appelOffre, () -> numero, parametres);
  }

  /**
   * Génère le contrat BROUILLON d'un marché attribué ; le numéro n'est réservé qu'après contrôle de
   * R03.
   *
   * @param appelOffre appel d'offres
   * @param numero fournisseur du numéro, appelé après validation
   * @param parametres paramètres configurables (pénalité par défaut)
   * @return le contrat au statut BROUILLON
   * @throws sn.cse.common.exception.BusinessRuleException R03 si l'appel d'offres n'est pas
   *     attribué
   */
  public static ContratAchat generer(
      AppelOffre appelOffre, Supplier<String> numero, ParametresContrat parametres) {
    // Règle R03 : un contrat ne peut naître que d'un appel d'offres attribué.
    Regles.exiger(
        appelOffre.getStatut() == StatutAO.ATTRIBUE,
        CodesErreur.AO_NON_ATTRIBUE,
        "Un contrat ne peut être généré que pour un appel d'offres attribué (R03).");
    Offre offre = appelOffre.offreRetenue().orElseThrow();
    Optional<OrigineBesoin> origine = appelOffre.getOrigine();
    return new ContratAchat(
        UUID.randomUUID(),
        numero.get(),
        appelOffre.getId(),
        appelOffre.getReference(),
        offre.getId(),
        offre.getFournisseurId(),
        offre.getFournisseurNom(),
        origine.map(OrigineBesoin::besoinId).orElse(null),
        origine.map(OrigineBesoin::chantierId).orElse(null),
        origine.map(OrigineBesoin::chantierNom).orElse(null),
        appelOffre.getObjet(),
        origine
            .map(OrigineBesoin::designation)
            .filter(d -> !d.isBlank())
            .orElse(appelOffre.getObjet()),
        appelOffre.getCategorie(),
        appelOffre.getQuantite(),
        appelOffre.getMontantAttribue(),
        StatutContrat.BROUILLON,
        null,
        offre.getDelaiLivraisonJours(),
        null,
        null,
        null,
        null,
        new ClausesContrat(
            offre.getGarantieMois(), null, null, parametres.penaliteRetardPctParJourDefaut()),
        null,
        List.of(),
        BigDecimal.ZERO,
        null,
        null,
        null,
        null,
        null,
        null,
        0);
  }

  /**
   * Met à jour les clauses et remplace les jalons (contrat BROUILLON uniquement).
   *
   * @param nouvellesClauses clauses
   * @param delai délai de livraison en jours
   * @param nouveauxJalons jalons
   * @throws ConflictException si le contrat n'est plus en brouillon
   */
  public void modifierClauses(
      ClausesContrat nouvellesClauses, int delai, List<NouveauJalon> nouveauxJalons) {
    if (statut != StatutContrat.BROUILLON) {
      throw new ConflictException(
          CodesErreur.CONTRAT_NON_MODIFIABLE,
          "Les clauses du contrat " + numero + " ne sont modifiables qu'au statut BROUILLON.");
    }
    Regles.exiger(
        delai >= 1, CodesErreur.DONNEE_INVALIDE, "Le délai de livraison doit être positif.");
    List<Jalon> remplacants = new ArrayList<>();
    for (NouveauJalon j : nouveauxJalons) {
      Regles.exiger(
          j.datePrevue() != null,
          CodesErreur.DONNEE_INVALIDE,
          "Chaque jalon doit avoir une date prévue.");
      remplacants.add(
          new Jalon(
              UUID.randomUUID(),
              Regles.texteObligatoire(j.libelle(), "libelle"),
              j.datePrevue(),
              null));
    }
    this.clauses = Objects.requireNonNull(nouvellesClauses, "clauses");
    this.delaiLivraisonJours = delai;
    jalons.clear();
    jalons.addAll(remplacants);
  }

  /**
   * Signature du CSE : les clauses sont figées et le contrat est soumis à la signature du
   * fournisseur (BROUILLON → EN_ATTENTE_SIGNATURE). Le motif d'un refus précédent reste visible
   * jusqu'à la signature du fournisseur.
   *
   * @param signataire responsable des achats
   * @param maintenant instant de signature
   * @throws ConflictException si le contrat n'est pas en brouillon
   */
  public void signerPourLeCse(Acteur signataire, Instant maintenant) {
    statut.exigerTransition(StatutContrat.EN_ATTENTE_SIGNATURE);
    statut = StatutContrat.EN_ATTENTE_SIGNATURE;
    signatureCse = Signature.par(signataire, maintenant);
  }

  /**
   * Signature du fournisseur sur le portail : le contrat entre en vigueur (SIGNE), l'échéance de
   * livraison est fixée et un engagement est demandé à la DAF (R06).
   *
   * @param signataire représentant du fournisseur
   * @param maintenant instant de signature
   * @throws ConflictException si le contrat n'attend pas la signature du fournisseur
   */
  public void signerPourLeFournisseur(Acteur signataire, Instant maintenant) {
    statut.exigerTransition(StatutContrat.SIGNE);
    statut = StatutContrat.SIGNE;
    signatureFournisseur = Signature.par(signataire, maintenant);
    motifRefusSignature = null;
    dateSignature = LocalDate.ofInstant(maintenant, ZoneOffset.UTC);
    dateLivraisonPrevue = dateSignature.plusDays(delaiLivraisonJours);
    engagement = Engagement.demander(montant, maintenant);
  }

  /**
   * Refus de signature du fournisseur : le contrat revient en BROUILLON pour renégociation, la
   * signature du CSE est annulée et le motif conservé à l'intention du responsable des achats.
   *
   * @param motif motif du refus (obligatoire)
   * @throws ConflictException si le contrat n'attend pas la signature du fournisseur
   * @throws sn.cse.common.exception.BusinessRuleException si le motif est absent
   */
  public void refuserSignature(String motif) {
    // SIGNE → BROUILLON existe aussi (refus DAF) : seul un contrat en attente de signature est
    // visé.
    if (statut != StatutContrat.EN_ATTENTE_SIGNATURE) {
      throw new ConflictException(
          CodesErreur.TRANSITION_INTERDITE,
          "Le contrat "
              + numero
              + " n'attend pas la signature du fournisseur (statut "
              + statut
              + ").");
    }
    Regles.exiger(
        motif != null && !motif.isBlank(),
        CodesErreur.MOTIF_OBLIGATOIRE,
        "Le motif du refus de signature est obligatoire.");
    statut = StatutContrat.BROUILLON;
    signatureCse = null;
    motifRefusSignature = motif.trim();
  }

  /**
   * Décision de la DAF sur l'engagement. Un refus renvoie le contrat en BROUILLON pour
   * renégociation.
   *
   * @param approuve décision
   * @param commentaire commentaire
   * @param daf décideur
   * @param maintenant instant
   * @throws ConflictException si aucun engagement n'est en attente
   */
  public void deciderEngagement(
      boolean approuve, String commentaire, Acteur daf, Instant maintenant) {
    if (statut != StatutContrat.SIGNE || engagement == null) {
      throw new ConflictException(
          CodesErreur.ENGAGEMENT_NON_EN_ATTENTE,
          "Aucun engagement en attente pour le contrat " + numero + " (statut " + statut + ").");
    }
    engagement = engagement.decider(approuve, daf, maintenant, Regles.facultatif(commentaire));
    StatutContrat cible = approuve ? StatutContrat.ENGAGEMENT_VALIDE : StatutContrat.BROUILLON;
    statut.exigerTransition(cible);
    statut = cible;
    if (!approuve) {
      // Renégociation : les deux parties devront signer à nouveau.
      dateSignature = null;
      dateLivraisonPrevue = null;
      signatureCse = null;
      signatureFournisseur = null;
    }
  }

  /**
   * Démarre la livraison.
   *
   * @throws sn.cse.common.exception.BusinessRuleException R06 si l'engagement n'est pas validé
   * @throws ConflictException si le contrat est déjà en livraison ou au-delà
   */
  public void demarrerLivraison() {
    // Règle R06 : pas de livraison sans engagement validé par la DAF.
    Regles.exiger(
        !statut.estAvantEngagementValide(),
        CodesErreur.ENGAGEMENT_NON_VALIDE,
        "La livraison ne peut démarrer qu'après validation de l'engagement par la DAF (R06).");
    statut.exigerTransition(StatutContrat.EN_LIVRAISON);
    statut = StatutContrat.EN_LIVRAISON;
  }

  /**
   * Expédition déclarée par le fournisseur sur le portail : la livraison démarre (mêmes règles que
   * {@link #demarrerLivraison()}, R06) et la date d'expédition est conservée.
   *
   * @param date date d'expédition
   * @param commentaire commentaire facultatif (transporteur, numéro de suivi...)
   * @param aujourdhui date du jour
   * @throws sn.cse.common.exception.BusinessRuleException R06 si l'engagement n'est pas validé, ou
   *     date d'expédition future ou antérieure à la signature
   */
  public void declarerExpedition(LocalDate date, String commentaire, LocalDate aujourdhui) {
    // Règle R06 vérifiée en premier : un fournisseur ne peut pas expédier sans engagement validé.
    Regles.exiger(
        !statut.estAvantEngagementValide(),
        CodesErreur.ENGAGEMENT_NON_VALIDE,
        "L'expédition ne peut être déclarée qu'après validation de l'engagement par la DAF (R06).");
    Regles.exiger(
        date != null && !date.isAfter(aujourdhui),
        CodesErreur.DATE_FUTURE,
        "La date d'expédition ne peut pas être dans le futur.");
    Regles.exiger(
        dateSignature == null || !date.isBefore(dateSignature),
        CodesErreur.DATE_EXPEDITION_INVALIDE,
        "La date d'expédition ne peut pas précéder la signature du contrat.");
    demarrerLivraison();
    dateExpedition = date;
    commentaireExpedition = Regles.facultatif(commentaire);
  }

  /**
   * Jalon renseigné par le fournisseur : uniquement une fois l'expédition déclarée (EN_LIVRAISON).
   *
   * @param jalonId jalon
   * @param dateReelle date de réalisation
   * @param aujourdhui date du jour
   * @throws ConflictException si le contrat n'est pas en livraison
   */
  public void enregistrerJalonDuFournisseur(
      UUID jalonId, LocalDate dateReelle, LocalDate aujourdhui) {
    if (statut != StatutContrat.EN_LIVRAISON) {
      throw new ConflictException(
          CodesErreur.CONTRAT_NON_EN_LIVRAISON,
          "Le fournisseur ne renseigne ses jalons qu'une fois l'expédition déclarée (contrat "
              + numero
              + " au statut "
              + statut
              + ").");
    }
    enregistrerJalon(jalonId, dateReelle, aujourdhui);
  }

  /**
   * Portail : le fournisseur ne facture qu'un contrat livré (ou clôturé).
   *
   * @throws sn.cse.common.exception.BusinessRuleException sinon
   */
  public void exigerLivrePourFacturationFournisseur() {
    Regles.exiger(
        statut == StatutContrat.LIVRE || statut == StatutContrat.CLOTURE,
        CodesErreur.CONTRAT_NON_LIVRE,
        "Une facture ne peut être déposée qu'une fois la livraison du contrat "
            + numero
            + " confirmée (statut actuel "
            + statut
            + ").");
  }

  /**
   * Enregistre la réalisation d'un jalon.
   *
   * @param jalonId jalon
   * @param dateReelle date de réalisation
   * @param aujourdhui date du jour
   */
  public void enregistrerJalon(UUID jalonId, LocalDate dateReelle, LocalDate aujourdhui) {
    if (!statut.estEnExecution()) {
      throw new ConflictException(
          CodesErreur.CONTRAT_NON_EN_EXECUTION,
          "Les jalons ne se renseignent qu'entre la signature et la livraison.");
    }
    Regles.exiger(
        dateReelle != null && !dateReelle.isAfter(aujourdhui),
        CodesErreur.DATE_FUTURE,
        "La date de réalisation ne peut pas être dans le futur.");
    int index = -1;
    for (int i = 0; i < jalons.size(); i++) {
      if (jalons.get(i).id().equals(jalonId)) {
        index = i;
      }
    }
    if (index < 0) {
      throw new ResourceNotFoundException("Jalon", jalonId);
    }
    jalons.set(index, jalons.get(index).atteintLe(dateReelle));
  }

  /**
   * Confirme la livraison et arrête les pénalités de retard.
   *
   * @param dateReelle date de livraison
   * @param bonLivraison numéro du bon de livraison
   * @param observations observations
   * @param aujourdhui date du jour
   * @param plafondPenalitesPct plafond des pénalités (% du montant)
   */
  public void confirmerLivraison(
      LocalDate dateReelle,
      String bonLivraison,
      String observations,
      LocalDate aujourdhui,
      BigDecimal plafondPenalitesPct) {
    statut.exigerTransition(StatutContrat.LIVRE);
    Regles.exiger(
        dateReelle != null && !dateReelle.isAfter(aujourdhui),
        CodesErreur.DATE_FUTURE,
        "La date de livraison ne peut pas être dans le futur.");
    Regles.exiger(
        !dateReelle.isBefore(dateSignature),
        CodesErreur.DATE_LIVRAISON_INVALIDE,
        "La date de livraison ne peut pas précéder la signature du contrat.");
    numeroBonLivraison = Regles.texteObligatoire(bonLivraison, "numeroBonLivraison");
    observationsLivraison = Regles.facultatif(observations);
    dateLivraisonReelle = dateReelle;
    penalitesRetard = penalitesAu(dateReelle, plafondPenalitesPct);
    statut = StatutContrat.LIVRE;
  }

  /** Clôture le contrat livré. */
  public void cloturer() {
    statut.exigerTransition(StatutContrat.CLOTURE);
    statut = StatutContrat.CLOTURE;
  }

  /**
   * @param aujourdhui date du jour
   * @return vrai si l'échéance de livraison est dépassée et le contrat non livré
   */
  public boolean estEnRetard(LocalDate aujourdhui) {
    return statut.estEnExecution()
        && dateLivraisonPrevue != null
        && aujourdhui.isAfter(dateLivraisonPrevue);
  }

  /**
   * Met à jour les pénalités courues d'un contrat en retard et indique si une alerte doit être
   * émise (au plus une par intervalle de rappel).
   *
   * @param aujourdhui date du jour
   * @param parametres paramètres (plafond, intervalle de rappel)
   * @return vrai si une alerte de retard doit être publiée
   */
  public boolean signalerRetard(LocalDate aujourdhui, ParametresContrat parametres) {
    if (!estEnRetard(aujourdhui)) {
      return false;
    }
    penalitesRetard = penalitesAu(aujourdhui, parametres.plafondPenalitesPct());
    boolean alerte =
        dateDerniereAlerteRetard == null
            || ChronoUnit.DAYS.between(dateDerniereAlerteRetard, aujourdhui)
                >= parametres.intervalleRappelRetardJours();
    if (alerte) {
      dateDerniereAlerteRetard = aujourdhui;
    }
    return alerte;
  }

  /**
   * @param reference date de référence (livraison effective ou date du jour)
   * @return jours de retard par rapport à l'échéance (0 si dans les délais)
   */
  public long joursRetard(LocalDate reference) {
    if (dateLivraisonPrevue == null || reference == null) {
      return 0;
    }
    return Math.max(0, ChronoUnit.DAYS.between(dateLivraisonPrevue, reference));
  }

  /**
   * Pénalités = montant × pénalité journalière (%) × jours de retard, plafonnées.
   *
   * @param reference date de référence
   * @param plafondPct plafond en % du montant
   * @return pénalités en FCFA (arrondies à l'unité)
   */
  public BigDecimal penalitesAu(LocalDate reference, BigDecimal plafondPct) {
    BigDecimal taux =
        clauses.penaliteRetardPctParJour() == null
            ? BigDecimal.ZERO
            : clauses.penaliteRetardPctParJour();
    BigDecimal brutes =
        montant.multiply(taux).multiply(BigDecimal.valueOf(joursRetard(reference))).divide(CENT);
    BigDecimal plafond = montant.multiply(plafondPct).divide(CENT);
    return brutes.min(plafond).setScale(0, RoundingMode.HALF_UP);
  }

  /**
   * @param taux fraction du prix unitaire facturée par jour d'utilisation
   * @return tarif journalier estimé d'une unité livrée (FCFA, arrondi au millier)
   */
  public BigDecimal tarifJournalierEstime(BigDecimal taux) {
    return montant
        .divide(BigDecimal.valueOf(Math.max(1, quantite)), 0, RoundingMode.HALF_UP)
        .multiply(taux)
        .setScale(-3, RoundingMode.HALF_UP)
        .setScale(0, RoundingMode.UNNECESSARY);
  }

  public UUID getId() {
    return id;
  }

  public String getNumero() {
    return numero;
  }

  public UUID getAppelOffreId() {
    return appelOffreId;
  }

  public String getAppelOffreReference() {
    return appelOffreReference;
  }

  public UUID getOffreId() {
    return offreId;
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public String getFournisseurNom() {
    return fournisseurNom;
  }

  public UUID getBesoinId() {
    return besoinId;
  }

  public UUID getChantierId() {
    return chantierId;
  }

  public String getChantierNom() {
    return chantierNom;
  }

  public String getObjet() {
    return objet;
  }

  public String getDesignation() {
    return designation;
  }

  public CategorieMateriel getCategorie() {
    return categorie;
  }

  public int getQuantite() {
    return quantite;
  }

  public BigDecimal getMontant() {
    return montant;
  }

  public StatutContrat getStatut() {
    return statut;
  }

  public LocalDate getDateSignature() {
    return dateSignature;
  }

  public int getDelaiLivraisonJours() {
    return delaiLivraisonJours;
  }

  public LocalDate getDateLivraisonPrevue() {
    return dateLivraisonPrevue;
  }

  public LocalDate getDateLivraisonReelle() {
    return dateLivraisonReelle;
  }

  public String getNumeroBonLivraison() {
    return numeroBonLivraison;
  }

  public String getObservationsLivraison() {
    return observationsLivraison;
  }

  public ClausesContrat getClauses() {
    return clauses;
  }

  public Optional<Engagement> getEngagement() {
    return Optional.ofNullable(engagement);
  }

  public List<Jalon> getJalons() {
    return Collections.unmodifiableList(jalons);
  }

  public BigDecimal getPenalitesRetard() {
    return penalitesRetard;
  }

  public LocalDate getDateDerniereAlerteRetard() {
    return dateDerniereAlerteRetard;
  }

  public Optional<Signature> getSignatureCse() {
    return Optional.ofNullable(signatureCse);
  }

  public Optional<Signature> getSignatureFournisseur() {
    return Optional.ofNullable(signatureFournisseur);
  }

  public String getMotifRefusSignature() {
    return motifRefusSignature;
  }

  public LocalDate getDateExpedition() {
    return dateExpedition;
  }

  public String getCommentaireExpedition() {
    return commentaireExpedition;
  }

  public long getVersion() {
    return version;
  }
}
