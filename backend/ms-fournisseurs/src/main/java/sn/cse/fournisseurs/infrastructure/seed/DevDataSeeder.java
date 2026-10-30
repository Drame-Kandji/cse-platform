package sn.cse.fournisseurs.infrastructure.seed;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.command.LitigeCommand;
import sn.cse.fournisseurs.application.port.in.SurveillanceFournisseursUseCase;
import sn.cse.fournisseurs.application.port.out.EvaluationRepository;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.application.service.CatalogueService;
import sn.cse.fournisseurs.application.service.ChargeurFournisseur;
import sn.cse.fournisseurs.application.service.LitigeService;
import sn.cse.fournisseurs.application.service.RecalculScore;
import sn.cse.fournisseurs.domain.model.Auteur;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.CoordonneesBancaires;
import sn.cse.fournisseurs.domain.model.DonneesLigneCatalogue;
import sn.cse.fournisseurs.domain.model.Evaluation;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.GraviteLitige;
import sn.cse.fournisseurs.domain.model.Litige;
import sn.cse.fournisseurs.domain.model.NotesEvaluation;
import sn.cse.fournisseurs.domain.model.ProfilFournisseur;
import sn.cse.fournisseurs.domain.model.TypeActionLitige;
import sn.cse.fournisseurs.domain.model.TypeDocument;
import sn.cse.fournisseurs.domain.service.ScorePolicy;

/**
 * Jeu de démonstration (profil {@code dev}, idempotent) : les 7 fournisseurs du contrat §12 (noms
 * fictifs), documents (dont certains expirés), évaluations réparties sur 9 mois, livraisons,
 * catalogues avec historique de prix, litiges et une demande de blacklistage en attente. Toutes les
 * écritures passent par le domaine et l'outbox afin d'alimenter reporting, notification et audit.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "cse.seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);

  static final UUID F1 = id(1);
  static final UUID F2 = id(2);
  static final UUID F3 = id(3);
  static final UUID F4 = id(4);
  static final UUID F5 = id(5);
  static final UUID F6 = id(6);
  static final UUID F7 = id(7);

  private static final Auteur RA =
      new Auteur(UUID.fromString("00000000-0000-0000-0000-000000000301"), "Fatou Sarr");
  private static final Auteur RM =
      new Auteur(UUID.fromString("00000000-0000-0000-0000-000000000201"), "Ibrahima Fall");
  private static final String SENEGAL = "Sénégal";
  private static final String UNITE = "unité";
  private static final String TONNE = "tonne";

  private final FournisseurRepository fournisseurs;
  private final EvaluationRepository evaluations;
  private final ChargeurFournisseur chargeur;
  private final CatalogueService catalogue;
  private final LitigeService litiges;
  private final RecalculScore recalcul;
  private final SurveillanceFournisseursUseCase surveillance;
  private final FournisseurEventPublisher evenements;
  private final ScorePolicy politique;
  private final TransactionTemplate transaction;
  private final Clock clock;

  /**
   * @param fournisseurs dépôt des fournisseurs
   * @param evaluations dépôt des évaluations
   * @param chargeur chargement des fournisseurs
   * @param catalogue service catalogue
   * @param litiges service litiges
   * @param recalcul recalcul du score
   * @param surveillance contrôles périodiques
   * @param evenements publication outbox
   * @param politique politique de score
   * @param transaction gabarit transactionnel
   * @param clock horloge
   */
  public DevDataSeeder(
      FournisseurRepository fournisseurs,
      EvaluationRepository evaluations,
      ChargeurFournisseur chargeur,
      CatalogueService catalogue,
      LitigeService litiges,
      RecalculScore recalcul,
      SurveillanceFournisseursUseCase surveillance,
      FournisseurEventPublisher evenements,
      ScorePolicy politique,
      TransactionTemplate transaction,
      Clock clock) {
    this.fournisseurs = fournisseurs;
    this.evaluations = evaluations;
    this.chargeur = chargeur;
    this.catalogue = catalogue;
    this.litiges = litiges;
    this.recalcul = recalcul;
    this.surveillance = surveillance;
    this.evenements = evenements;
    this.politique = politique;
    this.transaction = transaction;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (fournisseurs.parId(F1).isPresent()) {
      LOG.info("Jeu de démonstration fournisseurs déjà présent : rien à faire");
      return;
    }
    transaction.executeWithoutResult(statut -> semer());
    LOG.info("Jeu de démonstration fournisseurs créé (7 fournisseurs)");
  }

  private void semer() {
    referentiel();
    documents();
    evaluationsEtLivraisons();
    catalogues();
    litigesDemo();
    statuts();
    for (UUID id : List.of(F1, F2, F3, F4, F5, F6, F7)) {
      recalcul.recalculer(chargeur.charger(id), "Initialisation du jeu de démonstration");
    }
    surveillance.signalerDocumentsExpires();
  }

  // --- Référentiel ------------------------------------------------------------------------------

  private void referentiel() {
    creer(
        F1,
        new ProfilFournisseur(
            "Sahel Engins SA",
            "005123456 2G3",
            "SN-DKR-2015-B-12345",
            "Km 4,5 boulevard du Centenaire de la Commune",
            "Dakar",
            SENEGAL,
            "+221 33 849 12 34",
            "contact@sahel-engins.sn",
            "Abdoulaye Mbaye",
            "Directeur commercial",
            EnumSet.of(CategorieMateriel.MATERIEL_ROULANT),
            List.of("ISO 9001:2015", "Concessionnaire agréé engins TP")),
        new CoordonneesBancaires("Banque Sahélienne de Commerce", "SN08SN0100152000048500003035"),
        false,
        270);
    creer(
        F2,
        new ProfilFournisseur(
            "Teranga Équipements BTP",
            "006234567 2V1",
            "SN-DKR-2017-B-23456",
            "Zone industrielle de Mbao, route de Rufisque",
            "Dakar",
            SENEGAL,
            "+221 33 879 45 60",
            "commercial@teranga-equipements.sn",
            "Mariama Diallo",
            "Responsable grands comptes",
            EnumSet.of(CategorieMateriel.MATERIEL_ROULANT, CategorieMateriel.OUTILLAGE),
            List.of("ISO 9001:2015")),
        new CoordonneesBancaires("Crédit Teranga", "SN12K01001520000256000012345"),
        false,
        265);
    creer(
        F3,
        new ProfilFournisseur(
            "Dakar Outillage Pro",
            "004345678 1A2",
            "SN-DKR-2012-B-04567",
            "Avenue Blaise Diagne, Médina",
            "Dakar",
            SENEGAL,
            "+221 33 822 31 77",
            "ventes@dakar-outillage.sn",
            "Ousmane Gueye",
            "Gérant",
            EnumSet.of(CategorieMateriel.OUTILLAGE),
            List.of()),
        new CoordonneesBancaires("Banque Sahélienne de Commerce", "SN08SN0100152000099100004567"),
        false,
        260);
    creer(
        F4,
        new ProfilFournisseur(
            "Cap-Vert Informatique",
            "007456789 2C4",
            "SN-DKR-2019-B-34567",
            "Rue Carnot x Joseph Gomis, Plateau",
            "Dakar",
            SENEGAL,
            "+221 33 889 60 10",
            "pro@capvert-informatique.sn",
            "Aïssatou Faye",
            "Chargée d'affaires",
            EnumSet.of(CategorieMateriel.EQUIPEMENT_INFORMATIQUE),
            List.of("Partenaire constructeur Gold")),
        new CoordonneesBancaires("Crédit Teranga", "SN12K01001520000256000067890"),
        false,
        250);
    creer(
        F5,
        new ProfilFournisseur(
            "Atlantique Consommables SARL",
            "005567890 2B5",
            "SN-THS-2016-B-00412",
            "Route de Mbour, quartier Grand Thiès",
            "Thiès",
            SENEGAL,
            "+221 33 951 22 48",
            "commandes@atlantique-consommables.sn",
            "Pape Ndiaye",
            "Directeur des opérations",
            EnumSet.of(CategorieMateriel.CONSOMMABLE),
            List.of("Agrément distributeur ciment")),
        new CoordonneesBancaires("Banque du Cayor", "SN15C02002030000100200030040"),
        false,
        255);
    creer(
        F6,
        new ProfilFournisseur(
            "Ndiambour Matériaux",
            "003678901 1D6",
            "SN-THS-2014-B-00789",
            "Quartier Escale, avenue Léopold Sédar Senghor",
            "Thiès",
            SENEGAL,
            "+221 33 952 70 15",
            "contact@ndiambour-materiaux.sn",
            "Babacar Sy",
            "Gérant",
            EnumSet.of(CategorieMateriel.CONSOMMABLE, CategorieMateriel.OUTILLAGE),
            List.of()),
        new CoordonneesBancaires("Banque du Cayor", "SN15C02002030000100200099887"),
        false,
        268);
    creer(
        F7,
        new ProfilFournisseur(
            "Baobab Logistique",
            "008789012 2E7",
            "SN-DKR-2024-B-45678",
            "Cité Keur Gorgui, lot 112",
            "Dakar",
            SENEGAL,
            "+221 77 645 18 92",
            "inscription@baobab-logistique.sn",
            "Ndeye Fatou Cissé",
            "Fondatrice",
            EnumSet.of(CategorieMateriel.MATERIEL_ROULANT),
            List.of()),
        null,
        true,
        12);
  }

  private void creer(
      UUID id,
      ProfilFournisseur profil,
      CoordonneesBancaires coordonnees,
      boolean enAttente,
      int joursDepuisInscription) {
    Fournisseur fournisseur =
        Fournisseur.creerAvecIdentifiant(
            id,
            profil,
            coordonnees,
            enAttente,
            politique.scoreInitial(),
            aujourdhui().minusDays(joursDepuisInscription));
    evenements.publierFournisseur(
        EventTypes.FOURNISSEUR_INSCRIT, fournisseurs.sauvegarder(fournisseur), null);
  }

  // --- Documents --------------------------------------------------------------------------------

  private void documents() {
    LocalDate jour = aujourdhui();
    for (UUID id : List.of(F1, F2, F3, F4, F5, F6, F7)) {
      Fournisseur f = chargeur.charger(id);
      f.ajouterDocument(TypeDocument.NINEA, "Attestation NINEA", null);
      f.ajouterDocument(TypeDocument.RCCM, "Extrait RCCM " + f.getProfil().rccm(), null);
      fournisseurs.sauvegarder(f);
    }
    ajouter(F1, TypeDocument.QUITUS_FISCAL, "Quitus fiscal — DGID", jour.plusMonths(5));
    ajouter(F1, TypeDocument.ASSURANCE, "Assurance responsabilité civile", jour.plusMonths(8));
    ajouter(F1, TypeDocument.CERTIFICATION, "Certificat ISO 9001:2015", jour.plusYears(1));
    ajouter(F2, TypeDocument.QUITUS_FISCAL, "Quitus fiscal — DGID", jour.plusMonths(3));
    ajouter(F2, TypeDocument.ASSURANCE, "Assurance flotte engins", jour.plusDays(20));
    ajouter(F3, TypeDocument.QUITUS_FISCAL, "Quitus fiscal — DGID", jour.plusMonths(4));
    // Documents expirés : alimentent DOCUMENT_EXPIRE et le compteur documentsExpires.
    ajouter(F3, TypeDocument.ASSURANCE, "Assurance responsabilité civile", jour.minusDays(18));
    ajouter(F4, TypeDocument.QUITUS_FISCAL, "Quitus fiscal — DGID", jour.plusMonths(6));
    ajouter(F5, TypeDocument.QUITUS_FISCAL, "Quitus fiscal — DGID", jour.minusDays(5));
    ajouter(F5, TypeDocument.ASSURANCE, "Assurance transport de marchandises", jour.plusMonths(2));
    ajouter(F6, TypeDocument.QUITUS_FISCAL, "Quitus fiscal — DGID", jour.minusMonths(2));
    ajouter(F6, TypeDocument.ASSURANCE, "Assurance responsabilité civile", jour.minusMonths(1));
    ajouter(F7, TypeDocument.QUITUS_FISCAL, "Quitus fiscal — DGID", jour.plusMonths(10));
  }

  private void ajouter(UUID id, TypeDocument type, String libelle, LocalDate expiration) {
    Fournisseur f = chargeur.charger(id);
    f.ajouterDocument(type, libelle, expiration);
    fournisseurs.sauvegarder(f);
  }

  // --- Évaluations et livraisons ----------------------------------------------------------------

  private void evaluationsEtLivraisons() {
    // Notes [qualité, délais, conformité, SAV, prix] et ancienneté en jours (9 derniers mois).
    evaluer(
        F1,
        new int[][] {
          {8, 8, 9, 7, 7}, {9, 8, 9, 8, 7}, {8, 9, 9, 8, 8}, {9, 9, 9, 8, 7}, {9, 8, 10, 9, 8}
        },
        255,
        195,
        130,
        70,
        20);
    evaluer(
        F2,
        new int[][] {{7, 6, 8, 7, 8}, {7, 5, 7, 6, 8}, {8, 7, 8, 7, 8}, {7, 7, 8, 7, 9}},
        240,
        170,
        100,
        35);
    evaluer(F3, new int[][] {{8, 8, 8, 6, 9}, {7, 8, 8, 7, 9}, {8, 9, 8, 7, 8}}, 210, 120, 45);
    evaluer(F4, new int[][] {{9, 9, 9, 9, 7}, {9, 10, 9, 8, 7}, {8, 9, 9, 9, 8}}, 225, 150, 60);
    evaluer(
        F5,
        new int[][] {{6, 5, 7, 6, 8}, {6, 6, 6, 5, 8}, {7, 6, 7, 6, 7}, {6, 5, 6, 6, 8}},
        260,
        180,
        110,
        40);
    evaluer(
        F6,
        new int[][] {{5, 3, 4, 3, 7}, {4, 3, 4, 2, 7}, {4, 2, 3, 3, 6}, {3, 2, 3, 2, 6}},
        250,
        175,
        105,
        50);
    livrer(F1, 10, 9);
    livrer(F2, 8, 6);
    livrer(F3, 6, 5);
    livrer(F4, 4, 4);
    livrer(F5, 7, 5);
    livrer(F6, 6, 2);
  }

  private void evaluer(UUID id, int[][] notes, int... joursAvant) {
    Fournisseur f = chargeur.charger(id);
    for (int i = 0; i < notes.length; i++) {
      int[] n = notes[i];
      NotesEvaluation ne = new NotesEvaluation(n[0], n[1], n[2], n[3], n[4]);
      evaluations.enregistrer(
          Evaluation.creer(
              f,
              null,
              ne,
              politique.noteGlobale(ne),
              commentaire(ne),
              i % 2 == 0 ? RM : RA,
              ilYA(joursAvant[i])));
    }
  }

  private static String commentaire(NotesEvaluation n) {
    if (n.delais() <= 4) {
      return "Retards importants et matériel partiellement conforme";
    }
    if (n.qualite() >= 9) {
      return "Matériel de très bonne qualité, livraison sans réserve";
    }
    return "Prestation satisfaisante, quelques points d'amélioration sur le SAV";
  }

  private void livrer(UUID id, int total, int alHeure) {
    Fournisseur f = chargeur.charger(id);
    LocalDate base = aujourdhui().minusMonths(8);
    for (int i = 0; i < total; i++) {
      LocalDate prevue = base.plusDays(24L * i);
      LocalDate reelle = i < alHeure ? prevue.minusDays(i % 2) : prevue.plusDays(4L + i);
      f.enregistrerLivraison(prevue, reelle);
    }
    fournisseurs.sauvegarder(f);
  }

  // --- Catalogues -------------------------------------------------------------------------------

  private void catalogues() {
    CategorieMateriel roulant = CategorieMateriel.MATERIEL_ROULANT;
    CategorieMateriel outillage = CategorieMateriel.OUTILLAGE;
    CategorieMateriel info = CategorieMateriel.EQUIPEMENT_INFORMATIQUE;
    CategorieMateriel conso = CategorieMateriel.CONSOMMABLE;
    prix(
        F1,
        "SE-PH20",
        "Pelle hydraulique sur chenilles 20 t",
        roulant,
        UNITE,
        60,
        240,
        95_000_000,
        90,
        98_500_000);
    prix(
        F1,
        "SE-CB25",
        "Camion-benne 6x4 25 t",
        roulant,
        UNITE,
        45,
        240,
        61_000_000,
        120,
        63_500_000);
    prix(F1, "SE-NIV140", "Niveleuse 140 ch", roulant, UNITE, 75, 200, 112_000_000);
    prix(
        F2,
        "TEB-PEL20",
        "Pelle hydraulique sur chenilles 20 t",
        roulant,
        UNITE,
        50,
        230,
        97_000_000,
        60,
        96_200_000);
    prix(F2, "TEB-BEN25", "Camion-benne 6x4 25 t", roulant, UNITE, 40, 230, 64_800_000);
    prix(F2, "TEB-GE100", "Groupe électrogène 100 kVA", outillage, UNITE, 15, 210, 15_900_000);
    prix(F2, "TEB-MP30", "Marteau-piqueur pneumatique 30 kg", outillage, UNITE, 7, 210, 1_250_000);
    prix(
        F3,
        "DOP-GE100",
        "Groupe électrogène 100 kVA",
        outillage,
        UNITE,
        10,
        220,
        14_200_000,
        100,
        14_800_000);
    prix(F3, "DOP-PLQ90", "Plaque vibrante 90 kg", outillage, UNITE, 5, 220, 1_850_000);
    prix(F3, "DOP-BET350", "Bétonnière 350 L", outillage, UNITE, 5, 160, 1_100_000, 30, 1_150_000);
    prix(F3, "DOP-MP30", "Marteau-piqueur pneumatique 30 kg", outillage, UNITE, 4, 160, 1_190_000);
    prix(
        F4,
        "CVI-PC14",
        "Ordinateur portable 14 pouces i5 16 Go",
        info,
        UNITE,
        7,
        230,
        650_000,
        80,
        620_000);
    prix(F4, "CVI-GPS", "Station GNSS de topographie", info, UNITE, 21, 200, 9_800_000);
    prix(F4, "CVI-TAB", "Tablette durcie de chantier", info, UNITE, 10, 150, 540_000);
    prix(
        F5,
        "ATC-CEM425",
        "Ciment CEM II 42,5 R",
        conso,
        TONNE,
        3,
        250,
        78_000,
        150,
        82_000,
        40,
        85_000);
    prix(F5, "ATC-BIT70", "Bitume 70/100", conso, TONNE, 10, 250, 450_000, 90, 480_000);
    prix(F5, "ATC-GO", "Gasoil chantier", conso, "litre", 2, 120, 755);
    prix(F6, "NDM-CEM425", "Ciment CEM II 42,5 R", conso, TONNE, 5, 240, 76_000, 140, 80_000);
    prix(F6, "NDM-FER12", "Fer à béton HA12", conso, TONNE, 7, 240, 520_000);
    prix(F7, "BBL-PC60", "Location camion porte-char 60 t (jour)", roulant, "jour", 2, 10, 350_000);
  }

  /** Publie une ligne à plusieurs dates : paires (ancienneté en jours, prix) successives. */
  private void prix(
      UUID id,
      String reference,
      String designation,
      CategorieMateriel categorie,
      String unite,
      int delai,
      long... datesEtPrix) {
    Fournisseur f = chargeur.charger(id);
    for (int i = 0; i + 1 < datesEtPrix.length; i += 2) {
      catalogue.publier(
          f,
          new DonneesLigneCatalogue(
              reference,
              designation,
              categorie,
              BigDecimal.valueOf(datesEtPrix[i + 1]),
              unite,
              delai),
          ilYA((int) datesEtPrix[i]));
    }
  }

  // --- Litiges ----------------------------------------------------------------------------------

  private void litigesDemo() {
    Litige l1 =
        litiges.ouvrir(
            chargeur.charger(F2),
            new LitigeCommand(
                null,
                "Livraison incomplète de camions-bennes",
                "2 camions-bennes sur 5 livrés au chantier CH-THS-01 à la date prévue.",
                GraviteLitige.MOYENNE),
            RA,
            ilYA(180));
    litiges.agir(
        l1.getId(),
        TypeActionLitige.ACTION,
        "Mise en demeure adressée au fournisseur",
        RA,
        ilYA(175));
    litiges.agir(
        l1.getId(),
        TypeActionLitige.RESOLUTION,
        "Livraison du solde et pénalité de retard de 3 500 000 FCFA acceptée",
        RA,
        ilYA(160));

    Litige l2 =
        litiges.ouvrir(
            chargeur.charger(F6),
            new LitigeCommand(
                null,
                "Ciment non conforme à la norme CEM II 42,5",
                "Essais de laboratoire non conformes sur 3 lots livrés au chantier CH-KLK-01.",
                GraviteLitige.ELEVEE),
            RM,
            ilYA(95));
    litiges.agir(
        l2.getId(), TypeActionLitige.ACTION, "Expertise contradictoire demandée", RA, ilYA(88));

    litiges.ouvrir(
        chargeur.charger(F6),
        new LitigeCommand(
            null,
            "Facturation de quantités non livrées",
            "Écart de 40 tonnes entre les bons de livraison et la facture de septembre.",
            GraviteLitige.ELEVEE),
        RA,
        ilYA(30));

    Litige l4 =
        litiges.ouvrir(
            chargeur.charger(F5),
            new LitigeCommand(
                null,
                "Retard de livraison de bitume",
                "Livraison effectuée avec 6 jours de retard sur le chantier CH-DKR-01.",
                GraviteLitige.FAIBLE),
            RM,
            ilYA(150));
    litiges.agir(
        l4.getId(),
        TypeActionLitige.CLASSEMENT,
        "Retard imputable à la fermeture du port, classé sans suite",
        RA,
        ilYA(140));

    litiges.ouvrir(
        chargeur.charger(F1),
        new LitigeCommand(
            null,
            "Pièces de rechange manquantes",
            "Kit de flexibles hydrauliques absent à la livraison de la pelle SE-PH20.",
            GraviteLitige.FAIBLE),
        RM,
        ilYA(12));
  }

  // --- Statuts ----------------------------------------------------------------------------------

  private void statuts() {
    Fournisseur f6 = chargeur.charger(F6);
    String motifSuspension = "Litiges qualité répétés et documents administratifs expirés";
    f6.suspendre(motifSuspension);
    evenements.publierFournisseur(
        EventTypes.FOURNISSEUR_SUSPENDU, fournisseurs.sauvegarder(f6), motifSuspension);

    // Demande de blacklistage en attente de décision de la DG (R09, étape 1).
    f6 = chargeur.charger(F6);
    String motifBlacklistage =
        "Non-conformités graves non corrigées malgré la suspension (litiges ouverts)";
    f6.demanderBlacklistage(RA, motifBlacklistage, ilYA(3));
    evenements.publierFournisseur(
        EventTypes.FOURNISSEUR_BLACKLISTAGE_DEMANDE,
        fournisseurs.sauvegarder(f6),
        motifBlacklistage);
  }

  // --- Utilitaires ------------------------------------------------------------------------------

  private LocalDate aujourdhui() {
    return LocalDate.now(clock);
  }

  private Instant ilYA(int jours) {
    return clock.instant().minus(Duration.ofDays(jours));
  }

  private static UUID id(int n) {
    return UUID.fromString(String.format("f0000000-0000-0000-0000-%012d", n));
  }

  /**
   * @return identifiants des fournisseurs de démonstration
   */
  static Set<UUID> identifiants() {
    return Set.of(F1, F2, F3, F4, F5, F6, F7);
  }
}
