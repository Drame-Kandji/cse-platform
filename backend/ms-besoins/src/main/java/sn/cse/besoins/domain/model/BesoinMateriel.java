package sn.cse.besoins.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import sn.cse.besoins.domain.exception.BesoinNonModifiableException;
import sn.cse.besoins.domain.exception.CodesErreurBesoin;
import sn.cse.besoins.domain.exception.TransitionInterditeException;
import sn.cse.besoins.domain.validation.ChaineValidation;
import sn.cse.besoins.domain.validation.DecisionValidation;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.ForbiddenOperationException;

/**
 * Agrégat « besoin en matériel » exprimé par un chantier. Porte la machine à états {@link
 * StatutBesoin}, les règles R01, R05 et R08, l'historique complet des actions et les événements de
 * domaine à publier.
 *
 * <p>Invariant : un besoin {@code VALIDE} est toujours {@code orientationEnAttente}, il attend
 * d'être affecté directement ou transmis aux achats ; une interruption entre validation et
 * orientation laisse donc le besoin relançable.
 */
public final class BesoinMateriel {

  private final UUID id;
  private final String reference;
  private final UUID demandeurId;
  private final String demandeurNom;
  private final Instant creeLe;
  private final Long version;
  private final List<UUID> materielIdsAffectes;
  private final List<HistoriqueBesoin> historique;
  private final List<EvenementBesoin> evenements = new ArrayList<>();

  private UUID chantierId;
  private String chantierNom;
  private DescriptionBesoin description;
  private StatutBesoin statut;
  private String motifRejet;
  private UUID validateurId;
  private String validateurNom;
  private Instant dateValidation;
  private boolean orientationEnAttente;
  private Instant modifieLe;

  private BesoinMateriel(EtatBesoin etat) {
    this.id = Objects.requireNonNull(etat.id(), "id");
    this.reference = Objects.requireNonNull(etat.reference(), "reference");
    this.chantierId = Objects.requireNonNull(etat.chantierId(), "chantierId");
    this.chantierNom = etat.chantierNom();
    this.demandeurId = etat.demandeurId();
    this.demandeurNom = etat.demandeurNom();
    this.description = Objects.requireNonNull(etat.description(), "description");
    this.statut = Objects.requireNonNull(etat.statut(), "statut");
    this.motifRejet = etat.motifRejet();
    this.validateurId = etat.validateurId();
    this.validateurNom = etat.validateurNom();
    this.dateValidation = etat.dateValidation();
    this.materielIdsAffectes =
        new ArrayList<>(
            etat.materielIdsAffectes() == null ? List.of() : etat.materielIdsAffectes());
    this.orientationEnAttente = etat.orientationEnAttente();
    this.historique = new ArrayList<>(etat.historique() == null ? List.of() : etat.historique());
    this.historique.sort(Comparator.comparing(HistoriqueBesoin::date));
    this.version = etat.version();
    this.creeLe = Objects.requireNonNull(etat.creeLe(), "creeLe");
    this.modifieLe = etat.modifieLe() == null ? etat.creeLe() : etat.modifieLe();
  }

  /**
   * Soumission d'un nouveau besoin par un chef de chantier ou le Responsable Matériel.
   *
   * @param id identifiant généré par l'application
   * @param reference référence lisible attribuée
   * @param chantierId chantier concerné (vérifié actif par l'appelant)
   * @param chantierNom nom du chantier
   * @param demandeur auteur
   * @param description contenu (R08 déjà vérifiée par l'objet-valeur)
   * @param maintenant instant de soumission
   * @return le besoin {@code EN_ATTENTE}
   */
  public static BesoinMateriel soumettre(
      UUID id,
      String reference,
      UUID chantierId,
      String chantierNom,
      Acteur demandeur,
      DescriptionBesoin description,
      Instant maintenant) {
    verifierDateBesoin(description, maintenant);
    BesoinMateriel besoin =
        new BesoinMateriel(
            new EtatBesoin(
                id,
                reference,
                chantierId,
                chantierNom,
                demandeur.id(),
                demandeur.nom(),
                description,
                StatutBesoin.EN_ATTENTE,
                null,
                null,
                null,
                null,
                List.of(),
                false,
                List.of(),
                null,
                maintenant,
                maintenant));
    besoin.tracer(ActionHistorique.SOUMISSION, null, demandeur, null, maintenant);
    besoin.emettre(TypeEvenementBesoin.SOUMIS, maintenant);
    return besoin;
  }

  /**
   * @param etat état persistant
   * @return l'agrégat reconstitué (aucun événement en attente)
   */
  public static BesoinMateriel reconstituer(EtatBesoin etat) {
    return new BesoinMateriel(etat);
  }

  /**
   * Modification du contenu par le demandeur, tant que le besoin est en attente (R05).
   *
   * @param versionAttendue version connue du client (verrou optimiste), ignorée si {@code null}
   * @param nouveauChantierId chantier (éventuellement modifié, vérifié par l'appelant)
   * @param nouveauChantierNom nom du chantier
   * @param nouvelleDescription nouveau contenu
   * @param auteur auteur de la modification
   * @param maintenant instant
   */
  public void modifier(
      Long versionAttendue,
      UUID nouveauChantierId,
      String nouveauChantierNom,
      DescriptionBesoin nouvelleDescription,
      Acteur auteur,
      Instant maintenant) {
    verifierVersion(versionAttendue);
    if (auteur.id() == null || !auteur.id().equals(demandeurId)) {
      throw new ForbiddenOperationException(
          CodesErreurBesoin.MODIFICATION_RESERVEE_DEMANDEUR,
          "Seul le demandeur peut modifier le besoin " + reference + ".");
    }
    // Règle R05 : plus aucune modification dès la validation.
    if (!statut.estModifiable()) {
      throw new BesoinNonModifiableException(reference, statut);
    }
    verifierDateBesoin(nouvelleDescription, maintenant);
    this.chantierId = Objects.requireNonNull(nouveauChantierId, "chantierId");
    this.chantierNom = nouveauChantierNom;
    this.description = nouvelleDescription;
    tracer(ActionHistorique.MODIFICATION, null, auteur, null, maintenant);
    emettre(TypeEvenementBesoin.MODIFIE, maintenant);
  }

  /**
   * Fait franchir au besoin l'étape de validation en attente (chaîne de responsabilité, R01). Si
   * c'était la dernière étape requise, le besoin devient {@code VALIDE} et attend son orientation.
   *
   * @param chaine chaîne de validation configurée
   * @param validateur auteur
   * @param commentaire commentaire optionnel
   * @param maintenant instant
   * @return la décision de la chaîne
   */
  public DecisionValidation valider(
      ChaineValidation chaine, Acteur validateur, String commentaire, Instant maintenant) {
    exigerTransition(StatutBesoin.VALIDE);
    DecisionValidation decision = chaine.traiter(this, validateur);
    String etape = decision.etapeApprouvee().code();
    if (!decision.estFinale()) {
      tracer(ActionHistorique.APPROBATION_ETAPE, etape, validateur, commentaire, maintenant);
      return decision;
    }
    this.statut = StatutBesoin.VALIDE;
    this.validateurId = validateur.id();
    this.validateurNom = validateur.nom();
    this.dateValidation = maintenant;
    this.orientationEnAttente = true;
    tracer(ActionHistorique.VALIDATION, etape, validateur, commentaire, maintenant);
    emettre(TypeEvenementBesoin.VALIDE, maintenant);
    return decision;
  }

  /**
   * Rejet motivé par le détenteur du rôle de l'étape en attente (R01, R08).
   *
   * @param chaine chaîne de validation configurée
   * @param acteur auteur
   * @param motif motif obligatoire
   * @param maintenant instant
   */
  public void rejeter(ChaineValidation chaine, Acteur acteur, String motif, Instant maintenant) {
    exigerTransition(StatutBesoin.REJETE);
    // Règle R08 : un rejet doit toujours être motivé.
    if (motif == null || motif.isBlank()) {
      throw new BusinessRuleException(
          CodesErreurBesoin.MOTIF_REJET_OBLIGATOIRE,
          "Le motif du rejet est obligatoire (règle R08).");
    }
    chaine.verifierAutoriteDeRejet(this, acteur);
    this.statut = StatutBesoin.REJETE;
    this.motifRejet = motif.strip();
    this.validateurId = acteur.id();
    this.validateurNom = acteur.nom();
    this.dateValidation = maintenant;
    tracer(ActionHistorique.REJET, null, acteur, this.motifRejet, maintenant);
    emettre(TypeEvenementBesoin.REJETE, maintenant);
  }

  /**
   * @throws ConflictException si le besoin n'attend pas d'orientation
   */
  public void exigerOrientationEnAttente() {
    if (statut != StatutBesoin.VALIDE || !orientationEnAttente) {
      throw new ConflictException(
          CodesErreurBesoin.ORIENTATION_NON_REQUISE,
          "Le besoin " + reference + " (" + statut + ") n'attend aucune orientation.");
    }
  }

  /**
   * Affectation directe : les matériels fournis complètent la quantité demandée.
   *
   * @param nouveauxMateriels matériels affectés lors de cette orientation
   * @param acteur auteur
   * @param maintenant instant
   */
  public void affecterDirectement(
      Collection<UUID> nouveauxMateriels, Acteur acteur, Instant maintenant) {
    exigerOrientationEnAttente();
    exigerTransition(StatutBesoin.AFFECTE_DIRECT);
    ajouterMateriels(nouveauxMateriels);
    if (quantiteRestanteAAffecter() > 0) {
      throw new IllegalArgumentException(
          "Affectation incomplète : " + quantiteRestanteAAffecter() + " matériel(s) manquant(s)");
    }
    this.statut = StatutBesoin.AFFECTE_DIRECT;
    this.orientationEnAttente = false;
    tracer(
        ActionHistorique.AFFECTATION_DIRECTE,
        null,
        acteur,
        materielIdsAffectes.size() + " matériel(s) du parc affecté(s) au chantier.",
        maintenant);
    emettre(TypeEvenementBesoin.AFFECTE_DIRECT, maintenant);
  }

  /**
   * Transmission aux achats faute de matériel disponible en quantité suffisante.
   *
   * @param dejaAffectes matériels affectés lors de cette tentative (peut être vide)
   * @param acteur auteur
   * @param commentaire détail de la disponibilité constatée
   * @param maintenant instant
   */
  public void orienterVersAchat(
      Collection<UUID> dejaAffectes, Acteur acteur, String commentaire, Instant maintenant) {
    exigerOrientationEnAttente();
    exigerTransition(StatutBesoin.EN_ACHAT);
    ajouterMateriels(dejaAffectes);
    this.statut = StatutBesoin.EN_ACHAT;
    this.orientationEnAttente = false;
    tracer(ActionHistorique.ORIENTATION_ACHAT, null, acteur, commentaire, maintenant);
    emettre(TypeEvenementBesoin.EN_ACHAT, maintenant);
  }

  /**
   * L'orientation n'a pas pu aboutir (service de suivi indisponible, conflits) : le besoin reste
   * {@code VALIDE} et l'orientation devra être relancée. Les matériels déjà affectés sont
   * conservés.
   *
   * @param dejaAffectes matériels affectés avant l'interruption
   * @param acteur auteur
   * @param raison cause de l'interruption
   * @param maintenant instant
   */
  public void suspendreOrientation(
      Collection<UUID> dejaAffectes, Acteur acteur, String raison, Instant maintenant) {
    exigerOrientationEnAttente();
    ajouterMateriels(dejaAffectes);
    tracer(ActionHistorique.ORIENTATION_EN_ATTENTE, null, acteur, raison, maintenant);
  }

  /**
   * Trace l'affectation d'un matériel notifiée par ms-suivi (aucun changement d'état).
   *
   * @param libelle description de l'affectation
   * @param acteur auteur de l'affectation
   * @param maintenant instant de l'affectation
   */
  public void journaliserAffectationMateriel(String libelle, Acteur acteur, Instant maintenant) {
    tracer(ActionHistorique.MATERIEL_AFFECTE, null, acteur, libelle, maintenant);
  }

  /**
   * @return quantité restant à satisfaire par affectation
   */
  public int quantiteRestanteAAffecter() {
    return Math.max(0, description.quantite() - materielIdsAffectes.size());
  }

  /**
   * @param codeEtape code d'étape de validation
   * @return vrai si l'étape a déjà été franchie
   */
  public boolean aFranchiEtape(String codeEtape) {
    return historique.stream().anyMatch(h -> codeEtape.equals(h.etapeValidation()));
  }

  /**
   * Vérification du verrou optimiste côté domaine (version transmise par le client).
   *
   * @param versionAttendue version connue du client ({@code null} = non contrôlée)
   */
  public void verifierVersion(Long versionAttendue) {
    if (versionAttendue != null && !versionAttendue.equals(version)) {
      throw new ConflictException(
          CodesErreurBesoin.CONFLIT_CONCURRENCE,
          "Le besoin " + reference + " a été modifié entre-temps. Rechargez-le puis réessayez.");
    }
  }

  /**
   * @return les événements en attente, retirés de l'agrégat
   */
  public List<EvenementBesoin> extraireEvenements() {
    List<EvenementBesoin> extraits = List.copyOf(evenements);
    evenements.clear();
    return extraits;
  }

  /**
   * @return l'état persistant courant
   */
  public EtatBesoin etat() {
    return new EtatBesoin(
        id,
        reference,
        chantierId,
        chantierNom,
        demandeurId,
        demandeurNom,
        description,
        statut,
        motifRejet,
        validateurId,
        validateurNom,
        dateValidation,
        List.copyOf(materielIdsAffectes),
        orientationEnAttente,
        List.copyOf(historique),
        version,
        creeLe,
        modifieLe);
  }

  private void exigerTransition(StatutBesoin cible) {
    if (!statut.peutPasserA(cible)) {
      throw new TransitionInterditeException(reference, statut, cible);
    }
  }

  private void ajouterMateriels(Collection<UUID> materiels) {
    if (materiels == null) {
      return;
    }
    materiels.stream()
        .filter(Objects::nonNull)
        .filter(m -> !materielIdsAffectes.contains(m))
        .forEach(materielIdsAffectes::add);
  }

  private void tracer(
      ActionHistorique action, String etape, Acteur auteur, String commentaire, Instant date) {
    historique.add(HistoriqueBesoin.de(statut, action, etape, auteur, commentaire, date));
    this.modifieLe = date;
  }

  private void emettre(TypeEvenementBesoin type, Instant date) {
    evenements.add(new EvenementBesoin(type, date));
  }

  private static void verifierDateBesoin(DescriptionBesoin description, Instant maintenant) {
    LocalDate aujourdhui = LocalDate.ofInstant(maintenant, ZoneOffset.UTC);
    if (description.dateBesoin().isBefore(aujourdhui)) {
      throw new BusinessRuleException(
          CodesErreurBesoin.DATE_BESOIN_PASSEE,
          "La date de besoin ne peut pas être antérieure à aujourd'hui (" + aujourdhui + ").");
    }
  }

  /**
   * @return identifiant
   */
  public UUID id() {
    return id;
  }

  /**
   * @return référence lisible
   */
  public String reference() {
    return reference;
  }

  /**
   * @return chantier demandeur
   */
  public UUID chantierId() {
    return chantierId;
  }

  /**
   * @return nom du chantier
   */
  public String chantierNom() {
    return chantierNom;
  }

  /**
   * @return demandeur
   */
  public UUID demandeurId() {
    return demandeurId;
  }

  /**
   * @return nom du demandeur
   */
  public String demandeurNom() {
    return demandeurNom;
  }

  /**
   * @return contenu du besoin
   */
  public DescriptionBesoin description() {
    return description;
  }

  /**
   * @return statut courant
   */
  public StatutBesoin statut() {
    return statut;
  }

  /**
   * @return motif du rejet
   */
  public String motifRejet() {
    return motifRejet;
  }

  /**
   * @return auteur de la décision
   */
  public UUID validateurId() {
    return validateurId;
  }

  /**
   * @return nom de l'auteur de la décision
   */
  public String validateurNom() {
    return validateurNom;
  }

  /**
   * @return instant de la décision
   */
  public Instant dateValidation() {
    return dateValidation;
  }

  /**
   * @return matériels affectés (lecture seule)
   */
  public List<UUID> materielIdsAffectes() {
    return List.copyOf(materielIdsAffectes);
  }

  /**
   * @return vrai si l'orientation reste à faire
   */
  public boolean orientationEnAttente() {
    return orientationEnAttente;
  }

  /**
   * @return historique chronologique (lecture seule)
   */
  public List<HistoriqueBesoin> historique() {
    return List.copyOf(historique);
  }

  /**
   * @return version (verrou optimiste), {@code null} avant le premier enregistrement
   */
  public Long version() {
    return version;
  }

  /**
   * @return instant de soumission
   */
  public Instant creeLe() {
    return creeLe;
  }

  /**
   * @return instant de dernière modification
   */
  public Instant modifieLe() {
    return modifieLe;
  }
}
