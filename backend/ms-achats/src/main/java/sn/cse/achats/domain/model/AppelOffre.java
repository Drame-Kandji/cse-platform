package sn.cse.achats.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.exception.Regles;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.ResourceNotFoundException;

/**
 * Agrégat appel d'offres : publication, dépôt des offres (R02, R10, R14), clôture, évaluation et
 * circuit d'attribution (R11). Cycle de vie : PUBLIE → CLOTURE → ATTRIBUE.
 */
public final class AppelOffre {

  private static final int LONGUEUR_CODE = 6;

  private final UUID id;
  private final String reference;
  private final String objet;
  private final String description;
  private final TypeAppelOffre type;
  private final OrigineBesoin origine;
  private final CategorieMateriel categorie;
  private final int quantite;
  private final String specifications;
  private final BigDecimal budgetEstime;
  private final Instant datePublication;
  private final Instant dateLimite;
  private Instant dateCloture;
  private StatutAO statut;
  private final PonderationCriteres ponderation;
  private final List<FournisseurInvite> invites;
  private final List<Offre> offres;
  private Attribution attribution;
  private UUID offreRetenueId;
  private BigDecimal montantAttribue;
  private final Acteur creePar;
  private final long version;

  /**
   * Reconstitution depuis la persistance (aucune règle de création n'est rejouée).
   *
   * @param id identifiant
   * @param reference référence lisible
   * @param objet objet
   * @param description description
   * @param type procédure
   * @param origine besoin d'origine
   * @param categorie catégorie
   * @param quantite quantité
   * @param specifications spécifications
   * @param budgetEstime budget estimé
   * @param datePublication instant de publication
   * @param dateLimite date limite de dépôt
   * @param dateCloture instant de clôture
   * @param statut statut
   * @param ponderation critères
   * @param invites fournisseurs invités
   * @param offres offres déposées
   * @param attribution proposition d'attribution en cours ou retenue
   * @param offreRetenueId offre retenue
   * @param montantAttribue montant attribué
   * @param creePar auteur
   * @param version version de verrou optimiste
   */
  public AppelOffre(
      UUID id,
      String reference,
      String objet,
      String description,
      TypeAppelOffre type,
      OrigineBesoin origine,
      CategorieMateriel categorie,
      int quantite,
      String specifications,
      BigDecimal budgetEstime,
      Instant datePublication,
      Instant dateLimite,
      Instant dateCloture,
      StatutAO statut,
      PonderationCriteres ponderation,
      List<FournisseurInvite> invites,
      List<Offre> offres,
      Attribution attribution,
      UUID offreRetenueId,
      BigDecimal montantAttribue,
      Acteur creePar,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.reference = Objects.requireNonNull(reference, "reference");
    this.objet = objet;
    this.description = description;
    this.type = Objects.requireNonNull(type, "type");
    this.origine = origine;
    this.categorie = Objects.requireNonNull(categorie, "categorie");
    this.quantite = quantite;
    this.specifications = specifications;
    this.budgetEstime = budgetEstime;
    this.datePublication = Objects.requireNonNull(datePublication, "datePublication");
    this.dateLimite = Objects.requireNonNull(dateLimite, "dateLimite");
    this.dateCloture = dateCloture;
    this.statut = Objects.requireNonNull(statut, "statut");
    this.ponderation = Objects.requireNonNull(ponderation, "ponderation");
    this.invites = new ArrayList<>(invites == null ? List.of() : invites);
    this.offres = new ArrayList<>(offres == null ? List.of() : offres);
    this.attribution = attribution;
    this.offreRetenueId = offreRetenueId;
    this.montantAttribue = montantAttribue;
    this.creePar = Objects.requireNonNull(creePar, "creePar");
    this.version = version;
  }

  /**
   * Publie un nouvel appel d'offres.
   *
   * @param donnees données saisies
   * @param reference référence attribuée (AO-2026-001)
   * @param maintenant instant de publication
   * @return l'appel d'offres au statut PUBLIE
   * @throws sn.cse.common.exception.BusinessRuleException si une règle de publication est violée
   */
  public static AppelOffre publier(NouvelAppelOffre donnees, String reference, Instant maintenant) {
    return publier(donnees, () -> reference, maintenant);
  }

  /**
   * Publie un nouvel appel d'offres ; la référence n'est réservée qu'après validation des données
   * (une valeur de séquence consommée n'est jamais rendue).
   *
   * @param donnees données saisies
   * @param reference fournisseur de la référence, appelé après validation
   * @param maintenant instant de publication
   * @return l'appel d'offres au statut PUBLIE
   * @throws sn.cse.common.exception.BusinessRuleException si une règle de publication est violée
   */
  public static AppelOffre publier(
      NouvelAppelOffre donnees, Supplier<String> reference, Instant maintenant) {
    Objects.requireNonNull(donnees.type(), "type");
    Objects.requireNonNull(donnees.categorie(), "categorie");
    Objects.requireNonNull(donnees.ponderation(), "ponderation");
    String objet = Regles.texteObligatoire(donnees.objet(), "objet");
    Regles.exiger(
        donnees.quantite() >= 1,
        CodesErreur.DONNEE_INVALIDE,
        "La quantité doit être d'au moins une unité.");
    if (donnees.budgetEstime() != null) {
      Regles.montantPositif(donnees.budgetEstime(), "budgetEstime");
    }
    Regles.exiger(
        donnees.dateLimite() != null && donnees.dateLimite().isAfter(maintenant),
        CodesErreur.DATE_LIMITE_INVALIDE,
        "La date limite de dépôt des offres doit être postérieure à la publication.");
    Regles.exiger(
        donnees.type() != TypeAppelOffre.DEMANDE_PRIX || !donnees.invites().isEmpty(),
        CodesErreur.INVITES_OBLIGATOIRES,
        "Une demande de prix exige au moins un fournisseur invité.");
    List<FournisseurInvite> invites = new ArrayList<>();
    for (FournisseurConnu f : donnees.invites()) {
      // Règle R14 : un fournisseur suspendu ou blacklisté ne peut pas être invité.
      f.exigerEligible();
      if (invites.stream().noneMatch(i -> i.id().equals(f.id()))) {
        invites.add(f.commeInvite());
      }
    }
    return new AppelOffre(
        UUID.randomUUID(),
        reference.get(),
        objet,
        Regles.facultatif(donnees.description()),
        donnees.type(),
        donnees.origine(),
        donnees.categorie(),
        donnees.quantite(),
        Regles.facultatif(donnees.specifications()),
        donnees.budgetEstime(),
        maintenant,
        donnees.dateLimite(),
        null,
        StatutAO.PUBLIE,
        donnees.ponderation(),
        invites,
        List.of(),
        null,
        null,
        null,
        Objects.requireNonNull(donnees.creePar(), "creePar"),
        0);
  }

  /**
   * Enregistre l'offre d'un fournisseur.
   *
   * @param fournisseur fournisseur vérifié
   * @param montant montant proposé (FCFA)
   * @param delaiLivraisonJours délai de livraison
   * @param garantieMois garantie (facultatif)
   * @param descriptionOffre description (facultatif)
   * @param maintenant instant du dépôt
   * @return l'offre créée
   * @throws ConflictException si l'appel d'offres n'est plus publié ou si le fournisseur a déjà
   *     déposé une offre
   * @throws sn.cse.common.exception.BusinessRuleException R02 (date limite dépassée), R14
   *     (fournisseur non éligible) ou fournisseur non invité à une demande de prix
   */
  public Offre deposerOffre(
      FournisseurConnu fournisseur,
      BigDecimal montant,
      int delaiLivraisonJours,
      Integer garantieMois,
      String descriptionOffre,
      Instant maintenant) {
    exigerStatut(StatutAO.PUBLIE, CodesErreur.AO_NON_PUBLIE, "Les dépôts sont fermés.");
    // Règle R02 : aucune offre n'est acceptée après la date limite.
    Regles.exiger(
        !maintenant.isAfter(dateLimite),
        CodesErreur.DATE_LIMITE_DEPASSEE,
        "La date limite de dépôt des offres est dépassée (R02).");
    // Règle R14 : un fournisseur suspendu ou blacklisté ne peut pas soumissionner.
    fournisseur.exigerEligible();
    Regles.exiger(
        type != TypeAppelOffre.DEMANDE_PRIX || estInvite(fournisseur.id()),
        CodesErreur.FOURNISSEUR_NON_INVITE,
        "Seuls les fournisseurs invités peuvent répondre à une demande de prix.");
    if (offres.stream().anyMatch(o -> o.getFournisseurId().equals(fournisseur.id()))) {
      throw new ConflictException(
          CodesErreur.OFFRE_DEJA_DEPOSEE,
          "Ce fournisseur a déjà déposé une offre sur l'appel d'offres " + reference + ".");
    }
    Offre offre =
        new Offre(
            UUID.randomUUID(),
            nouveauCodeAnonyme(),
            fournisseur.id(),
            fournisseur.raisonSociale(),
            montant,
            delaiLivraisonJours,
            garantieMois,
            descriptionOffre,
            maintenant,
            Map.of(),
            false);
    offres.add(offre);
    return offre;
  }

  /**
   * Clôture les dépôts et lève l'anonymat des offres (R10).
   *
   * @param maintenant instant de clôture
   * @throws ConflictException si l'appel d'offres n'est pas publié
   */
  public void cloturer(Instant maintenant) {
    exigerStatut(StatutAO.PUBLIE, CodesErreur.AO_NON_PUBLIE, "Il est déjà clôturé.");
    statut.exigerTransition(StatutAO.CLOTURE);
    statut = StatutAO.CLOTURE;
    dateCloture = maintenant;
  }

  /**
   * @param maintenant instant courant
   * @return vrai si l'appel d'offres est publié et sa date limite atteinte (clôture automatique)
   */
  public boolean estEchu(Instant maintenant) {
    return statut == StatutAO.PUBLIE && !maintenant.isBefore(dateLimite);
  }

  /**
   * Règle R10 : tant que l'appel d'offres est publié, l'identité des soumissionnaires est masquée.
   *
   * @return vrai si les offres doivent être présentées anonymisées
   */
  public boolean estAnonyme() {
    return statut == StatutAO.PUBLIE;
  }

  /**
   * Saisit les notes des critères non calculés d'une offre (PRIX et DELAI sont calculés).
   *
   * @param offreId offre évaluée
   * @param notes notes 0..100
   * @return l'offre notée
   * @throws ConflictException si l'appel d'offres n'est pas clôturé ou si une attribution est en
   *     cours
   * @throws sn.cse.common.exception.BusinessRuleException si une note est invalide
   */
  public Offre evaluerOffre(UUID offreId, Map<CritereEvaluation, Integer> notes) {
    exigerStatut(StatutAO.CLOTURE, CodesErreur.AO_NON_CLOTURE, "L'évaluation exige un AO clôturé.");
    if (attribution != null) {
      throw new ConflictException(
          CodesErreur.ATTRIBUTION_EN_COURS,
          "Une attribution est en cours d'approbation : les notes sont figées.");
    }
    Offre offre = offre(offreId);
    Map<CritereEvaluation, Integer> retenues = new EnumMap<>(CritereEvaluation.class);
    notes.forEach(
        (critere, note) -> {
          Regles.exiger(
              !critere.estCalcule(),
              CodesErreur.CRITERE_CALCULE,
              "Le critère " + critere + " est calculé automatiquement.");
          Regles.exiger(
              ponderation.contient(critere),
              CodesErreur.CRITERE_NON_RETENU,
              "Le critère " + critere + " ne fait pas partie de la grille de cet appel d'offres.");
          Regles.exiger(
              note != null && note >= 0 && note <= 100,
              CodesErreur.NOTE_INVALIDE,
              "La note du critère " + critere + " doit être comprise entre 0 et 100.");
          retenues.put(critere, note);
        });
    offre.noter(retenues);
    return offre;
  }

  /**
   * Vérifie que les offres peuvent être comparées (anonymat levé).
   *
   * @throws ConflictException si l'appel d'offres est encore publié
   */
  public void exigerEvaluable() {
    if (statut == StatutAO.PUBLIE) {
      throw new ConflictException(
          CodesErreur.AO_NON_CLOTURE,
          "Le comparatif n'est disponible qu'après la clôture de l'appel d'offres.");
    }
  }

  /**
   * Propose l'attribution du marché à une offre (étape 1 de R11).
   *
   * @param offreId offre proposée
   * @param justification justification obligatoire
   * @param auteur Responsable des Achats
   * @param maintenant instant
   * @param seuilDelegation seuil au-delà duquel la Direction Générale doit approuver
   * @return la proposition créée
   * @throws ConflictException si l'AO n'est pas clôturé ou si une proposition est déjà en cours
   */
  public Attribution proposerAttribution(
      UUID offreId,
      String justification,
      Acteur auteur,
      Instant maintenant,
      BigDecimal seuilDelegation) {
    if (statut == StatutAO.ATTRIBUE) {
      throw new ConflictException(
          CodesErreur.AO_DEJA_ATTRIBUE, "L'appel d'offres " + reference + " est déjà attribué.");
    }
    exigerStatut(
        StatutAO.CLOTURE, CodesErreur.AO_NON_CLOTURE, "L'attribution exige un AO clôturé.");
    if (attribution != null) {
      throw new ConflictException(
          CodesErreur.ATTRIBUTION_EN_COURS,
          "Une proposition d'attribution est déjà en cours d'approbation.");
    }
    Regles.exiger(
        justification != null && !justification.isBlank(),
        CodesErreur.JUSTIFICATION_OBLIGATOIRE,
        "La justification de l'attribution est obligatoire (R11).");
    Offre offre = offre(offreId);
    // Règle R11 : au-delà du seuil de délégation, la Direction Générale doit approuver.
    boolean dgRequise = offre.getMontantPropose().compareTo(seuilDelegation) > 0;
    attribution =
        new Attribution(
            offreId,
            offre.getMontantPropose(),
            justification.trim(),
            auteur,
            maintenant,
            Approbation.enAttente(),
            dgRequise ? Approbation.enAttente() : null);
    return attribution;
  }

  /**
   * Co-validation du Responsable Matériel (R11, toujours requise).
   *
   * @param approuve décision
   * @param commentaire commentaire
   * @param decideur Responsable Matériel
   * @param maintenant instant
   * @return l'issue de la décision
   */
  public ResultatDecision coValiderAttribution(
      boolean approuve, String commentaire, Acteur decideur, Instant maintenant) {
    Attribution courante = attributionEnCours();
    Approbation decision =
        courante.coValidationMateriel().decider(approuve, decideur, maintenant, commentaire);
    attribution = courante.avecCoValidation(decision);
    return conclure(approuve);
  }

  /**
   * Approbation de la Direction Générale (R11, requise au-delà du seuil de délégation).
   *
   * @param approuve décision
   * @param commentaire commentaire
   * @param decideur Directeur Général
   * @param maintenant instant
   * @return l'issue de la décision
   * @throws ConflictException si l'approbation DG n'est pas requise pour ce montant
   */
  public ResultatDecision approuverAttribution(
      boolean approuve, String commentaire, Acteur decideur, Instant maintenant) {
    Attribution courante = attributionEnCours();
    if (!courante.approbationDgRequise()) {
      throw new ConflictException(
          CodesErreur.APPROBATION_DG_NON_REQUISE,
          "Le montant ne dépasse pas le seuil de délégation : l'approbation DG n'est pas requise.");
    }
    Approbation decision =
        courante.approbationDg().decider(approuve, decideur, maintenant, commentaire);
    attribution = courante.avecApprobationDg(decision);
    return conclure(approuve);
  }

  /**
   * Vérifie qu'un procès-verbal peut être produit.
   *
   * @param typePv type demandé
   * @throws ConflictException OUVERTURE avant clôture, EVALUATION avant attribution
   */
  public void exigerProcesVerbalDisponible(TypeProcesVerbal typePv) {
    boolean disponible =
        typePv == TypeProcesVerbal.OUVERTURE
            ? statut != StatutAO.PUBLIE
            : statut == StatutAO.ATTRIBUE;
    if (!disponible) {
      throw new ConflictException(
          CodesErreur.PV_INDISPONIBLE,
          "Le procès-verbal "
              + typePv.name().toLowerCase(Locale.ROOT)
              + " n'est pas disponible au statut "
              + statut
              + ".");
    }
  }

  /**
   * @param offreId identifiant
   * @return l'offre
   * @throws ResourceNotFoundException si l'offre n'appartient pas à cet appel d'offres
   */
  public Offre offre(UUID offreId) {
    return offres.stream()
        .filter(o -> o.getId().equals(offreId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Offre", offreId));
  }

  /**
   * @return l'offre retenue si le marché est attribué
   */
  public Optional<Offre> offreRetenue() {
    return offreRetenueId == null ? Optional.empty() : Optional.of(offre(offreRetenueId));
  }

  /**
   * @param fournisseurId fournisseur
   * @return vrai si le fournisseur est invité
   */
  public boolean estInvite(UUID fournisseurId) {
    return invites.stream().anyMatch(i -> i.id().equals(fournisseurId));
  }

  /**
   * @param fournisseurId fournisseur
   * @return vrai si le fournisseur peut consulter cet appel d'offres sur le portail
   */
  public boolean estOuvertAuFournisseur(UUID fournisseurId) {
    return type == TypeAppelOffre.APPEL_OFFRES || estInvite(fournisseurId);
  }

  /**
   * @param fournisseurId fournisseur
   * @return l'offre déposée par ce fournisseur, s'il y en a une
   */
  public Optional<Offre> offreDu(UUID fournisseurId) {
    return offres.stream().filter(o -> o.getFournisseurId().equals(fournisseurId)).findFirst();
  }

  private ResultatDecision conclure(boolean approuve) {
    if (!approuve) {
      // Règle R11 : un refus supprime la proposition, le RA peut en formuler une nouvelle.
      attribution = null;
      return ResultatDecision.ATTRIBUTION_REFUSEE;
    }
    if (!attribution.estCompletementApprouvee()) {
      return ResultatDecision.EN_ATTENTE_AUTRES_APPROBATIONS;
    }
    statut.exigerTransition(StatutAO.ATTRIBUE);
    statut = StatutAO.ATTRIBUE;
    offreRetenueId = attribution.offreId();
    montantAttribue = attribution.montant();
    offre(offreRetenueId).retenir();
    return ResultatDecision.MARCHE_ATTRIBUE;
  }

  private Attribution attributionEnCours() {
    if (attribution == null || statut != StatutAO.CLOTURE) {
      throw new ConflictException(
          CodesErreur.AUCUNE_ATTRIBUTION, "Aucune proposition d'attribution n'est en cours.");
    }
    return attribution;
  }

  private void exigerStatut(StatutAO attendu, String code, String precision) {
    if (statut != attendu) {
      throw new ConflictException(
          code,
          "Opération impossible : l'appel d'offres "
              + reference
              + " est au statut "
              + statut
              + ". "
              + precision);
    }
  }

  private String nouveauCodeAnonyme() {
    String code;
    do {
      code =
          "OFR-"
              + UUID.randomUUID()
                  .toString()
                  .replace("-", "")
                  .substring(0, LONGUEUR_CODE)
                  .toUpperCase(Locale.ROOT);
    } while (codeUtilise(code));
    return code;
  }

  private boolean codeUtilise(String code) {
    return offres.stream().anyMatch(o -> o.getCodeAnonyme().equals(code));
  }

  public UUID getId() {
    return id;
  }

  public String getReference() {
    return reference;
  }

  public String getObjet() {
    return objet;
  }

  public String getDescription() {
    return description;
  }

  public TypeAppelOffre getType() {
    return type;
  }

  public Optional<OrigineBesoin> getOrigine() {
    return Optional.ofNullable(origine);
  }

  public CategorieMateriel getCategorie() {
    return categorie;
  }

  public int getQuantite() {
    return quantite;
  }

  public String getSpecifications() {
    return specifications;
  }

  public BigDecimal getBudgetEstime() {
    return budgetEstime;
  }

  public Instant getDatePublication() {
    return datePublication;
  }

  public Instant getDateLimite() {
    return dateLimite;
  }

  public Instant getDateCloture() {
    return dateCloture;
  }

  public StatutAO getStatut() {
    return statut;
  }

  public PonderationCriteres getPonderation() {
    return ponderation;
  }

  public List<FournisseurInvite> getInvites() {
    return Collections.unmodifiableList(invites);
  }

  public List<Offre> getOffres() {
    return Collections.unmodifiableList(offres);
  }

  public Optional<Attribution> getAttribution() {
    return Optional.ofNullable(attribution);
  }

  public UUID getOffreRetenueId() {
    return offreRetenueId;
  }

  public BigDecimal getMontantAttribue() {
    return montantAttribue;
  }

  public Acteur getCreePar() {
    return creePar;
  }

  public long getVersion() {
    return version;
  }
}
