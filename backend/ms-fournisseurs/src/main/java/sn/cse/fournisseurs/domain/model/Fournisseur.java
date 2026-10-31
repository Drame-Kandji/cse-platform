package sn.cse.fournisseurs.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.fournisseurs.domain.exception.CodesErreur;
import sn.cse.fournisseurs.domain.exception.TransitionInterditeException;

/**
 * Agrégat racine « Fournisseur » : référentiel légal, cycle de vie (machine à états {@link
 * StatutFournisseur}), blacklistage en deux étapes (R09), documents administratifs et indicateurs
 * de performance (R07).
 */
public final class Fournisseur {

  private static final String ENTITE = "fournisseur";

  private final UUID id;
  private ProfilFournisseur profil;
  private CoordonneesBancaires coordonneesBancaires;
  private StatutFournisseur statut;
  private String motifStatut;
  private DemandeBlacklistage demandeBlacklistage;
  private Performance performance;
  private final List<DocumentAdministratif> documents;
  private final LocalDate dateInscription;
  private final long version;

  private Fournisseur(
      UUID id,
      ProfilFournisseur profil,
      CoordonneesBancaires coordonneesBancaires,
      StatutFournisseur statut,
      String motifStatut,
      DemandeBlacklistage demandeBlacklistage,
      Performance performance,
      List<DocumentAdministratif> documents,
      LocalDate dateInscription,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.profil = Objects.requireNonNull(profil, "profil");
    this.coordonneesBancaires = coordonneesBancaires;
    this.statut = Objects.requireNonNull(statut, "statut");
    this.motifStatut = motifStatut;
    this.demandeBlacklistage = demandeBlacklistage;
    this.performance = Objects.requireNonNull(performance, "performance");
    this.documents = new ArrayList<>(documents == null ? List.of() : documents);
    this.dateInscription = Objects.requireNonNull(dateInscription, "dateInscription");
    this.version = version;
  }

  /**
   * Référence directement un fournisseur (création par le Responsable des Achats) : statut {@code
   * ACTIF}.
   *
   * @param profil informations légales
   * @param coordonneesBancaires coordonnées bancaires (optionnelles)
   * @param scoreInitial score attribué avant toute évaluation
   * @param dateInscription date d'inscription
   * @return nouveau fournisseur actif
   */
  public static Fournisseur referencer(
      ProfilFournisseur profil,
      CoordonneesBancaires coordonneesBancaires,
      BigDecimal scoreInitial,
      LocalDate dateInscription) {
    return nouveau(
        profil, coordonneesBancaires, StatutFournisseur.ACTIF, scoreInitial, dateInscription);
  }

  /**
   * Enregistre une inscription en attente de validation (auto-inscription portail, SPEC D7).
   *
   * @param profil informations légales
   * @param coordonneesBancaires coordonnées bancaires (optionnelles)
   * @param scoreInitial score attribué avant toute évaluation
   * @param dateInscription date d'inscription
   * @return nouveau fournisseur {@code EN_ATTENTE_VALIDATION}
   */
  public static Fournisseur inscrire(
      ProfilFournisseur profil,
      CoordonneesBancaires coordonneesBancaires,
      BigDecimal scoreInitial,
      LocalDate dateInscription) {
    return nouveau(
        profil,
        coordonneesBancaires,
        StatutFournisseur.EN_ATTENTE_VALIDATION,
        scoreInitial,
        dateInscription);
  }

  /**
   * Variante à identifiant imposé (référentiel partagé entre services, jeu de démonstration).
   *
   * @param id identifiant fixe
   * @param profil informations légales
   * @param coordonneesBancaires coordonnées bancaires (optionnelles)
   * @param enAttenteValidation {@code true} pour une inscription à valider, sinon ACTIF
   * @param scoreInitial score initial
   * @param dateInscription date d'inscription
   * @return nouveau fournisseur
   */
  public static Fournisseur creerAvecIdentifiant(
      UUID id,
      ProfilFournisseur profil,
      CoordonneesBancaires coordonneesBancaires,
      boolean enAttenteValidation,
      BigDecimal scoreInitial,
      LocalDate dateInscription) {
    return nouveau(
        id,
        profil,
        coordonneesBancaires,
        enAttenteValidation ? StatutFournisseur.EN_ATTENTE_VALIDATION : StatutFournisseur.ACTIF,
        scoreInitial,
        dateInscription);
  }

  private static Fournisseur nouveau(
      ProfilFournisseur profil,
      CoordonneesBancaires coordonneesBancaires,
      StatutFournisseur statut,
      BigDecimal scoreInitial,
      LocalDate dateInscription) {
    return nouveau(
        UUID.randomUUID(), profil, coordonneesBancaires, statut, scoreInitial, dateInscription);
  }

  private static Fournisseur nouveau(
      UUID id,
      ProfilFournisseur profil,
      CoordonneesBancaires coordonneesBancaires,
      StatutFournisseur statut,
      BigDecimal scoreInitial,
      LocalDate dateInscription) {
    return new Fournisseur(
        id,
        profil,
        coordonneesBancaires,
        statut,
        null,
        null,
        Performance.initiale(scoreInitial),
        List.of(),
        dateInscription,
        0L);
  }

  /**
   * Reconstitue l'agrégat depuis la persistance (aucune règle rejouée).
   *
   * @param etat état persistant complet
   * @return agrégat
   */
  public static Fournisseur reconstituer(EtatFournisseur etat) {
    return new Fournisseur(
        etat.id(),
        etat.profil(),
        etat.coordonneesBancaires(),
        etat.statut(),
        etat.motifStatut(),
        etat.demandeBlacklistage(),
        etat.performance(),
        etat.documents(),
        etat.dateInscription(),
        etat.version());
  }

  /**
   * Vérifie que la version fournie par le client correspond à l'état courant (verrou optimiste).
   *
   * @param versionClient version transmise par le client
   */
  public void verifierVersion(long versionClient) {
    if (versionClient != version) {
      throw new ConflictException(
          CodesErreur.CONFLIT_CONCURRENCE,
          "Ce fournisseur a été modifié par un autre utilisateur. Rechargez puis réessayez.");
    }
  }

  /**
   * Met à jour le référentiel. Les coordonnées bancaires absentes sont conservées.
   *
   * @param nouveauProfil nouvelles informations légales
   * @param nouvellesCoordonnees nouvelles coordonnées bancaires ({@code null} = inchangées)
   */
  public void modifierProfil(
      ProfilFournisseur nouveauProfil, CoordonneesBancaires nouvellesCoordonnees) {
    verifierNonBlackliste();
    this.profil = Objects.requireNonNull(nouveauProfil, "profil");
    if (nouvellesCoordonnees != null) {
      this.coordonneesBancaires = nouvellesCoordonnees;
    }
  }

  /** Valide une inscription : {@code EN_ATTENTE_VALIDATION → ACTIF}. */
  public void validerInscription() {
    if (statut != StatutFournisseur.EN_ATTENTE_VALIDATION) {
      throw new TransitionInterditeException(ENTITE, statut, StatutFournisseur.ACTIF);
    }
    changerStatut(StatutFournisseur.ACTIF, null);
  }

  /**
   * Suspend un fournisseur actif : {@code ACTIF → SUSPENDU}.
   *
   * @param motif motif obligatoire
   */
  public void suspendre(String motif) {
    String motifValide = motifObligatoire(motif);
    changerStatut(StatutFournisseur.SUSPENDU, motifValide);
  }

  /** Réactive un fournisseur suspendu : {@code SUSPENDU → ACTIF}. */
  public void reactiver() {
    if (statut != StatutFournisseur.SUSPENDU) {
      throw new TransitionInterditeException(ENTITE, statut, StatutFournisseur.ACTIF);
    }
    if (demandeBlacklistage != null) {
      throw new ConflictException(
          CodesErreur.DEMANDE_BLACKLISTAGE_EN_COURS,
          "Une demande de blacklistage est en attente de décision de la Direction Générale.");
    }
    changerStatut(StatutFournisseur.ACTIF, null);
  }

  /**
   * Règle R09, étape 1 : le Responsable des Achats demande le blacklistage. La transition vers
   * {@code BLACKLISTE} doit être autorisée depuis le statut courant (fournisseur suspendu).
   *
   * @param demandeur auteur de la demande
   * @param motif motif obligatoire
   * @param date instant de la demande
   */
  public void demanderBlacklistage(Auteur demandeur, String motif, Instant date) {
    String motifValide = motifObligatoire(motif);
    if (!statut.peutPasserA(StatutFournisseur.BLACKLISTE)) {
      throw new TransitionInterditeException(ENTITE, statut, StatutFournisseur.BLACKLISTE);
    }
    if (demandeBlacklistage != null) {
      throw new ConflictException(
          CodesErreur.DEMANDE_BLACKLISTAGE_EN_COURS,
          "Une demande de blacklistage est déjà en attente de décision.");
    }
    this.demandeBlacklistage =
        new DemandeBlacklistage(demandeur.id(), demandeur.nom(), motifValide, date);
  }

  /**
   * Règle R09, étape 2 : la Direction Générale approuve ou refuse la demande en attente.
   *
   * @param decideur membre de la Direction Générale
   * @param approuve {@code true} pour blacklister
   * @param commentaire commentaire de décision (optionnel)
   * @return la demande arbitrée
   */
  public DemandeBlacklistage deciderBlacklistage(
      Auteur decideur, boolean approuve, String commentaire) {
    DemandeBlacklistage demande = demandeBlacklistage;
    if (demande == null) {
      throw new ConflictException(
          CodesErreur.AUCUNE_DEMANDE_BLACKLISTAGE,
          "Aucune demande de blacklistage n'est en attente pour ce fournisseur.");
    }
    if (decideur.id() != null && decideur.id().equals(demande.demandeurId())) {
      throw new BusinessRuleException(
          CodesErreur.DECISION_PAR_DEMANDEUR_INTERDITE,
          "Le demandeur du blacklistage ne peut pas statuer sur sa propre demande.");
    }
    if (approuve) {
      String motif =
          commentaire == null || commentaire.isBlank()
              ? demande.motif()
              : demande.motif() + " · " + commentaire.trim();
      changerStatut(StatutFournisseur.BLACKLISTE, motif);
    }
    this.demandeBlacklistage = null;
    return demande;
  }

  /**
   * Ajoute un document administratif.
   *
   * @param type type
   * @param libelle libellé
   * @param dateExpiration date d'expiration (optionnelle)
   * @return document créé
   */
  public DocumentAdministratif ajouterDocument(
      TypeDocument type, String libelle, LocalDate dateExpiration) {
    verifierNonBlackliste();
    DocumentAdministratif document = DocumentAdministratif.creer(type, libelle, dateExpiration);
    documents.add(document);
    return document;
  }

  /**
   * Marque comme notifiés les documents expirés non encore signalés.
   *
   * @param aujourdhui date de référence
   * @return documents nouvellement signalés
   */
  public List<DocumentAdministratif> signalerDocumentsExpires(LocalDate aujourdhui) {
    List<DocumentAdministratif> aSignaler =
        documents.stream().filter(d -> d.expirationANotifier(aujourdhui)).toList();
    aSignaler.forEach(DocumentAdministratif::marquerExpirationNotifiee);
    return aSignaler;
  }

  /**
   * @param aujourdhui date de référence
   * @return nombre de documents expirés
   */
  public int nombreDocumentsExpires(LocalDate aujourdhui) {
    return (int) documents.stream().filter(d -> d.estExpire(aujourdhui)).count();
  }

  /** Vérifie que le fournisseur peut recevoir une évaluation (pas en attente de validation). */
  public void verifierEvaluable() {
    if (statut == StatutFournisseur.EN_ATTENTE_VALIDATION) {
      throw new BusinessRuleException(
          CodesErreur.FOURNISSEUR_NON_EVALUABLE,
          "Un fournisseur en attente de validation ne peut pas être évalué.");
    }
  }

  /**
   * Vérifie qu'une ligne de catalogue de cette catégorie peut être publiée.
   *
   * @param categorie catégorie de la ligne
   */
  public void verifierCatalogueModifiable(CategorieMateriel categorie) {
    verifierNonBlackliste();
    if (!profil.couvre(categorie)) {
      throw new BusinessRuleException(
          CodesErreur.CATEGORIE_HORS_SPECIALITES,
          "La catégorie " + categorie + " ne fait pas partie des spécialités du fournisseur.");
    }
  }

  /**
   * Prend en compte une livraison pour le taux de ponctualité.
   *
   * @param prevue date de livraison prévue
   * @param reelle date de livraison réelle
   * @return {@code true} si la livraison a été comptabilisée (deux dates connues)
   */
  public boolean enregistrerLivraison(LocalDate prevue, LocalDate reelle) {
    if (prevue == null || reelle == null) {
      return false;
    }
    this.performance = performance.avecLivraison(!reelle.isAfter(prevue));
    return true;
  }

  /**
   * Règle R07 : remplace le score par la valeur recalculée.
   *
   * @param score nouveau score 0..100
   * @param nombreEvaluations nombre d'évaluations prises en compte
   * @return {@code true} si le score a changé
   */
  public boolean actualiserScore(BigDecimal score, int nombreEvaluations) {
    boolean change =
        performance.scorePerformance().compareTo(score) != 0
            || performance.nombreEvaluations() != nombreEvaluations;
    this.performance = performance.avecScore(score, nombreEvaluations);
    return change;
  }

  private void changerStatut(StatutFournisseur cible, String motif) {
    if (!statut.peutPasserA(cible)) {
      throw new TransitionInterditeException(ENTITE, statut, cible);
    }
    this.statut = cible;
    this.motifStatut = motif;
  }

  private void verifierNonBlackliste() {
    if (statut == StatutFournisseur.BLACKLISTE) {
      throw new ConflictException(
          CodesErreur.FOURNISSEUR_BLACKLISTE, "Un fournisseur blacklisté n'est plus modifiable.");
    }
  }

  private static String motifObligatoire(String motif) {
    if (motif == null || motif.isBlank()) {
      throw new BusinessRuleException(CodesErreur.MOTIF_OBLIGATOIRE, "Le motif est obligatoire.");
    }
    return motif.trim();
  }

  public UUID getId() {
    return id;
  }

  public ProfilFournisseur getProfil() {
    return profil;
  }

  public Optional<CoordonneesBancaires> getCoordonneesBancaires() {
    return Optional.ofNullable(coordonneesBancaires);
  }

  public StatutFournisseur getStatut() {
    return statut;
  }

  public Optional<String> getMotifStatut() {
    return Optional.ofNullable(motifStatut);
  }

  public Optional<DemandeBlacklistage> getDemandeBlacklistage() {
    return Optional.ofNullable(demandeBlacklistage);
  }

  public Performance getPerformance() {
    return performance;
  }

  public List<DocumentAdministratif> getDocuments() {
    return List.copyOf(documents);
  }

  public LocalDate getDateInscription() {
    return dateInscription;
  }

  public long getVersion() {
    return version;
  }

  /**
   * État persistant complet de l'agrégat, utilisé pour la reconstitution.
   *
   * @param id identifiant
   * @param profil informations légales
   * @param coordonneesBancaires coordonnées bancaires
   * @param statut statut
   * @param motifStatut motif du statut courant
   * @param demandeBlacklistage demande en attente
   * @param performance indicateurs
   * @param documents documents administratifs
   * @param dateInscription date d'inscription
   * @param version version (verrou optimiste)
   */
  public record EtatFournisseur(
      UUID id,
      ProfilFournisseur profil,
      CoordonneesBancaires coordonneesBancaires,
      StatutFournisseur statut,
      String motifStatut,
      DemandeBlacklistage demandeBlacklistage,
      Performance performance,
      List<DocumentAdministratif> documents,
      LocalDate dateInscription,
      long version) {}
}
