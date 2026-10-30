package sn.cse.suivi.infrastructure.seed;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.config.SuiviParametres;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.ChantierRepository;
import sn.cse.suivi.application.port.out.GenerateurSequence;
import sn.cse.suivi.application.port.out.MaintenanceRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.PanneRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.port.out.TypeEnginRepository;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.ContexteMateriel;
import sn.cse.suivi.domain.model.Coordonnees;
import sn.cse.suivi.domain.model.DescriptionMateriel;
import sn.cse.suivi.domain.model.DonneesChantier;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.GravitePanne;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.Panne;
import sn.cse.suivi.domain.model.Periode;
import sn.cse.suivi.domain.model.TypeEngin;
import sn.cse.suivi.domain.model.TypeMaintenance;
import sn.cse.suivi.domain.model.TypeMouvement;
import sn.cse.suivi.domain.service.PolitiqueAffectation;

/**
 * Jeu de données de démonstration (profil {@code dev}, docs/API_CONTRACT.md §12). Idempotent : ne
 * fait rien si le premier chantier existe déjà. Passe par les agrégats du domaine (dates passées
 * fournies explicitement) et par l'outbox, afin que reporting, notification et audit reçoivent de
 * vrais événements répartis sur les 9 derniers mois.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "cse.seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);

  /** Identifiants partagés des chantiers (contrat §12). */
  static final List<UUID> CHANTIERS =
      List.of(
          uuid("c0000000-0000-0000-0000-000000000001"),
          uuid("c0000000-0000-0000-0000-000000000002"),
          uuid("c0000000-0000-0000-0000-000000000003"),
          uuid("c0000000-0000-0000-0000-000000000004"),
          uuid("c0000000-0000-0000-0000-000000000005"),
          uuid("c0000000-0000-0000-0000-000000000006"),
          uuid("c0000000-0000-0000-0000-000000000007"));

  private static final Mouvement.Auteur RM =
      new Mouvement.Auteur(uuid("00000000-0000-0000-0000-000000000201"), "Ibrahima Fall");
  private static final Mouvement.Auteur CHEF_DIOP =
      new Mouvement.Auteur(uuid("00000000-0000-0000-0000-000000000101"), "Mamadou Diop");
  private static final Mouvement.Auteur CHEF_NDIAYE =
      new Mouvement.Auteur(uuid("00000000-0000-0000-0000-000000000102"), "Awa Ndiaye");
  private static final Mouvement.Auteur DG =
      new Mouvement.Auteur(uuid("00000000-0000-0000-0000-000000000501"), "Aminata Sow");

  /** Parc central de Rufisque : position des matériels au dépôt. */
  private static final Coordonnees DEPOT = new Coordonnees(14.7236, -17.2733);

  private static final String[] PANNES = {
    "Fuite hydraulique sur le vérin de flèche",
    "Surchauffe moteur, voyant de température allumé",
    "Démarreur défaillant, démarrage impossible",
    "Alternateur hors service, batterie déchargée",
    "Chenille détendue, galet porteur usé",
    "Crevaison et jante avant endommagée",
    "Patinage de l'embrayage en charge",
    "Écran de la console de commande défaillant"
  };

  private static final String[] TECHNICIENS = {
    "Atelier central Rufisque — Moustapha Gueye",
    "Atelier central Rufisque — Ousseynou Faye",
    "Sahel Engins SA — SAV",
    "Teranga Équipements BTP — SAV"
  };

  /** Scénario de l'état final d'un matériel. */
  enum Final {
    EN_COURS,
    FIN_PROCHE,
    ATTENTE_RECEPTION,
    DISPONIBLE,
    MAINTENANCE_EN_COURS,
    MAINTENANCE_SUR_CHANTIER,
    MAINTENANCE_PLANIFIEE,
    PANNE_OUVERTE,
    REFORME,
    REFORME_DEMANDEE
  }

  /**
   * Fiche d'un engin de démonstration.
   *
   * @param code code parc
   * @param designation désignation
   * @param categorie catégorie
   * @param type rang du type d'engin (1..n) ou 0
   * @param marque marque
   * @param modele modèle
   * @param immatriculation immatriculation
   * @param tarif tarif journalier (FCFA)
   * @param valeur valeur d'acquisition (FCFA)
   * @param ageMois ancienneté
   * @param heures compteur à l'entrée dans le parc
   * @param chantier rang du chantier actuel (1..6) ou 0
   * @param etat scénario final
   */
  record Engin(
      String code,
      String designation,
      CategorieMateriel categorie,
      int type,
      String marque,
      String modele,
      String immatriculation,
      long tarif,
      long valeur,
      int ageMois,
      long heures,
      int chantier,
      Final etat) {}

  private final ChantierRepository chantiers;
  private final TypeEnginRepository types;
  private final MaterielRepository materiels;
  private final AffectationRepository affectations;
  private final MaintenanceRepository maintenances;
  private final PanneRepository pannes;
  private final MouvementRepository mouvements;
  private final GenerateurSequence sequences;
  private final SuiviEventPublisher evenements;
  private final SuiviParametres parametres;
  private final Clock clock;
  private final TransactionTemplate transaction;

  /**
   * @param chantiers stockage des chantiers
   * @param types stockage des types d'engins
   * @param materiels stockage des matériels
   * @param affectations stockage des affectations
   * @param maintenances stockage des ordres de travail
   * @param pannes stockage des pannes
   * @param mouvements journal des mouvements
   * @param sequences numérotation
   * @param evenements publication des événements
   * @param parametres paramètres métier
   * @param clock horloge
   * @param transactions gestionnaire de transactions
   */
  public DevDataSeeder(
      ChantierRepository chantiers,
      TypeEnginRepository types,
      MaterielRepository materiels,
      AffectationRepository affectations,
      MaintenanceRepository maintenances,
      PanneRepository pannes,
      MouvementRepository mouvements,
      GenerateurSequence sequences,
      SuiviEventPublisher evenements,
      SuiviParametres parametres,
      Clock clock,
      PlatformTransactionManager transactions) {
    this.chantiers = chantiers;
    this.types = types;
    this.materiels = materiels;
    this.affectations = affectations;
    this.maintenances = maintenances;
    this.pannes = pannes;
    this.mouvements = mouvements;
    this.sequences = sequences;
    this.evenements = evenements;
    this.parametres = parametres;
    this.clock = clock;
    this.transaction = new TransactionTemplate(transactions);
  }

  @Override
  public void run(ApplicationArguments args) {
    Boolean fait = transaction.execute(statut -> amorcer());
    LOG.info(
        Boolean.TRUE.equals(fait)
            ? "Données de démonstration ms-suivi créées"
            : "Données de démonstration ms-suivi déjà présentes");
  }

  /**
   * Crée le jeu de données s'il est absent (à appeler dans une transaction).
   *
   * @return vrai si des données ont été créées
   */
  boolean amorcer() {
    if (chantiers.findById(CHANTIERS.get(0)).isPresent()) {
      return false;
    }
    LocalDate aujourdhui = LocalDate.now(clock);
    Map<Integer, Chantier> parRang = creerChantiers(aujourdhui);
    Map<Integer, TypeEngin> parType = creerTypes();
    List<Engin> engins = engins();
    for (int i = 0; i < engins.size(); i++) {
      new Historique(i, engins.get(i), parRang, parType, aujourdhui).derouler();
    }
    Chantier mbour = parRang.get(7);
    mbour.terminer(affectations.compterActivesParChantier(mbour.getId()));
    evenements.chantier(EventTypes.CHANTIER_MODIFIE, chantiers.save(mbour));
    return true;
  }

  private Map<Integer, Chantier> creerChantiers(LocalDate t) {
    UUID diop = CHEF_DIOP.id();
    UUID ndiaye = CHEF_NDIAYE.id();
    List<DonneesChantier> donnees =
        List.of(
            chantier(
                "CH-DKR-01",
                "Extension autoroute Dakar–AIBD",
                "Diamniadio",
                "Dakar",
                14.7167,
                -17.4677,
                18_500_000_000L,
                21_000_000_000L,
                t.minusDays(420),
                t.plusDays(430),
                diop,
                CHEF_DIOP.nom()),
            chantier(
                "CH-THS-01",
                "Contournement de Thiès RN2",
                "Thiès",
                "Thiès",
                14.7910,
                -16.9256,
                9_800_000_000L,
                11_200_000_000L,
                t.minusDays(360),
                t.plusDays(250),
                diop,
                CHEF_DIOP.nom()),
            chantier(
                "CH-STL-01",
                "Pont de la lagune",
                "Saint-Louis",
                "Saint-Louis",
                16.0326,
                -16.4818,
                6_500_000_000L,
                7_400_000_000L,
                t.minusDays(330),
                t.plusDays(320),
                ndiaye,
                CHEF_NDIAYE.nom()),
            chantier(
                "CH-KLK-01",
                "Réhabilitation route Kaolack–Fatick",
                "Kaolack",
                "Kaolack",
                14.1652,
                -16.0758,
                12_300_000_000L,
                13_900_000_000L,
                t.minusDays(390),
                t.plusDays(380),
                ndiaye,
                CHEF_NDIAYE.nom()),
            chantier(
                "CH-ZIG-01",
                "Bâtiment administratif",
                "Ziguinchor",
                "Ziguinchor",
                12.5681,
                -16.2719,
                2_400_000_000L,
                2_850_000_000L,
                t.minusDays(300),
                t.plusDays(200),
                null,
                null),
            chantier(
                "CH-TBA-01",
                "Aménagement hydraulique",
                "Touba",
                "Diourbel",
                14.8500,
                -15.8833,
                4_700_000_000L,
                5_300_000_000L,
                t.minusDays(310),
                t.plusDays(290),
                null,
                null),
            chantier(
                "CH-MBR-01",
                "Station d'épuration",
                "Mbour",
                "Thiès",
                14.4199,
                -16.9658,
                3_100_000_000L,
                3_600_000_000L,
                t.minusDays(700),
                t.minusDays(30),
                null,
                null));
    Map<Integer, Chantier> parRang = new LinkedHashMap<>();
    for (int i = 0; i < donnees.size(); i++) {
      Chantier c = chantiers.save(Chantier.ouvrir(CHANTIERS.get(i), donnees.get(i)));
      evenements.chantier(EventTypes.CHANTIER_CREE, c);
      parRang.put(i + 1, c);
    }
    return parRang;
  }

  private Map<Integer, TypeEngin> creerTypes() {
    CategorieMateriel roulant = CategorieMateriel.MATERIEL_ROULANT;
    CategorieMateriel outillage = CategorieMateriel.OUTILLAGE;
    CategorieMateriel informatique = CategorieMateriel.EQUIPEMENT_INFORMATIQUE;
    List<DonneesTypeEngin> donnees =
        List.of(
            new DonneesTypeEngin("PELLE_HYD", "Pelle hydraulique", roulant, 500, 180, true),
            new DonneesTypeEngin("BULLDOZER", "Bulldozer", roulant, 500, 180, true),
            new DonneesTypeEngin("CAMION_BENNE", "Camion benne", roulant, 400, 120, true),
            new DonneesTypeEngin("NIVELEUSE", "Niveleuse", roulant, 500, 180, true),
            new DonneesTypeEngin("COMPACTEUR", "Compacteur", roulant, 400, 180, true),
            new DonneesTypeEngin("GRUE_MOBILE", "Grue mobile", roulant, 300, 90, true),
            new DonneesTypeEngin("CAMION_MALAXEUR", "Camion malaxeur", roulant, 400, 120, true),
            new DonneesTypeEngin("CHARGEUSE", "Chargeuse et tractopelle", roulant, 500, 180, true),
            new DonneesTypeEngin(
                "GROUPE_ELECTROGENE", "Groupe électrogène", outillage, 250, 90, true),
            new DonneesTypeEngin("PLAQUE_VIBRANTE", "Plaque vibrante", outillage, 200, 180, true),
            new DonneesTypeEngin(
                "STATION_TOTALE", "Station totale topographique", outillage, null, 365, true),
            new DonneesTypeEngin(
                "ORDINATEUR_PORTABLE", "Ordinateur portable", informatique, null, 365, true),
            new DonneesTypeEngin("TABLETTE", "Tablette de chantier", informatique, null, 365, true),
            new DonneesTypeEngin("DUMPER", "Dumper articulé", roulant, 500, 180, false));
    Map<Integer, TypeEngin> parRang = new LinkedHashMap<>();
    for (int i = 0; i < donnees.size(); i++) {
      UUID id = uuid(String.format("e0000000-0000-0000-0000-%012d", i + 1));
      parRang.put(i + 1, types.save(TypeEngin.creer(id, donnees.get(i))));
    }
    return parRang;
  }

  private static List<Engin> engins() {
    CategorieMateriel r = CategorieMateriel.MATERIEL_ROULANT;
    CategorieMateriel o = CategorieMateriel.OUTILLAGE;
    CategorieMateriel inf = CategorieMateriel.EQUIPEMENT_INFORMATIQUE;
    CategorieMateriel c = CategorieMateriel.CONSOMMABLE;
    String pelle20 = "Pelle hydraulique sur chenilles 20 t";
    String benne25 = "Camion benne 6x4 25 t";
    String pc = "Ordinateur portable bureau de chantier";
    String tablette = "Tablette de chantier durcie";
    return List.of(
        new Engin(
            "PEL-CAT320-01",
            pelle20,
            r,
            1,
            "Caterpillar",
            "320 GC",
            "DK-4521-BH",
            350_000,
            145_000_000,
            30,
            3200,
            1,
            Final.EN_COURS),
        new Engin(
            "PEL-CAT320-02",
            pelle20,
            r,
            1,
            "Caterpillar",
            "320 GC",
            "DK-4522-BH",
            350_000,
            148_000_000,
            18,
            1900,
            2,
            Final.EN_COURS),
        new Engin(
            "PEL-CAT323-03",
            "Pelle hydraulique sur chenilles 23 t",
            r,
            1,
            "Caterpillar",
            "323",
            "DK-6110-BK",
            390_000,
            172_000_000,
            12,
            900,
            4,
            Final.PANNE_OUVERTE),
        new Engin(
            "PEL-VOL-04",
            "Pelle sur pneus 15 t",
            r,
            1,
            "Volvo",
            "EWR150E",
            "DK-3305-BF",
            300_000,
            118_000_000,
            40,
            5200,
            0,
            Final.DISPONIBLE),
        new Engin(
            "BUL-KOM-01",
            "Bulldozer sur chenilles 20 t",
            r,
            2,
            "Komatsu",
            "D65EX-18",
            "DK-2201-BC",
            420_000,
            185_000_000,
            36,
            4600,
            1,
            Final.EN_COURS),
        new Engin(
            "BUL-KOM-02",
            "Bulldozer sur chenilles 37 t",
            r,
            2,
            "Komatsu",
            "D155AX-8",
            "DK-2202-BC",
            560_000,
            265_000_000,
            24,
            3100,
            4,
            Final.MAINTENANCE_PLANIFIEE),
        new Engin(
            "BUL-CAT-03",
            "Bulldozer sur chenilles 21 t",
            r,
            2,
            "Caterpillar",
            "D6R",
            "DK-7740-BD",
            430_000,
            190_000_000,
            60,
            9800,
            0,
            Final.REFORME),
        new Engin(
            "CAM-VOL-01",
            benne25,
            r,
            3,
            "Volvo",
            "FMX 440",
            "DK-5501-BE",
            180_000,
            68_000_000,
            26,
            4100,
            1,
            Final.EN_COURS),
        new Engin(
            "CAM-VOL-02",
            benne25,
            r,
            3,
            "Volvo",
            "FMX 440",
            "DK-5502-BE",
            180_000,
            68_000_000,
            26,
            4300,
            2,
            Final.FIN_PROCHE),
        new Engin(
            "CAM-VOL-03",
            benne25,
            r,
            3,
            "Volvo",
            "FMX 440",
            "DK-5503-BE",
            180_000,
            69_500_000,
            14,
            2200,
            3,
            Final.EN_COURS),
        new Engin(
            "CAM-REN-04",
            "Camion benne 6x4 20 t",
            r,
            3,
            "Renault Trucks",
            "Kerax 430",
            "DK-9012-BG",
            165_000,
            59_000_000,
            48,
            7600,
            0,
            Final.MAINTENANCE_EN_COURS),
        new Engin(
            "CAM-MER-05",
            "Camion benne 8x4 30 t",
            r,
            3,
            "Mercedes-Benz",
            "Actros 4141",
            "DK-1188-BL",
            195_000,
            76_000_000,
            8,
            900,
            6,
            Final.ATTENTE_RECEPTION),
        new Engin(
            "NIV-CAT-01",
            "Niveleuse 140 ch",
            r,
            4,
            "Caterpillar",
            "140K",
            "DK-3390-BH",
            380_000,
            162_000_000,
            34,
            4400,
            4,
            Final.EN_COURS),
        new Engin(
            "NIV-CAT-02",
            "Niveleuse 120 ch",
            r,
            4,
            "Caterpillar",
            "120",
            "DK-3391-BH",
            340_000,
            139_000_000,
            16,
            1700,
            2,
            Final.EN_COURS),
        new Engin(
            "COM-BOM-01",
            "Compacteur monocylindre 12 t",
            r,
            5,
            "Bomag",
            "BW 213 D-5",
            "DK-4410-BJ",
            160_000,
            58_000_000,
            28,
            3900,
            1,
            Final.EN_COURS),
        new Engin(
            "COM-DYN-02",
            "Compacteur tandem 10 t",
            r,
            5,
            "Dynapac",
            "CC4200",
            "DK-4411-BJ",
            150_000,
            54_000_000,
            20,
            2600,
            4,
            Final.MAINTENANCE_SUR_CHANTIER),
        new Engin(
            "COM-HAM-03",
            "Compacteur à pneus 24 t",
            r,
            5,
            "Hamm",
            "GRW 280",
            "DK-4412-BJ",
            170_000,
            61_000_000,
            44,
            6100,
            0,
            Final.DISPONIBLE),
        new Engin(
            "GRU-LIE-01",
            "Grue mobile tout-terrain 60 t",
            r,
            6,
            "Liebherr",
            "LTM 1060-3.1",
            "DK-8001-BM",
            650_000,
            420_000_000,
            30,
            3800,
            3,
            Final.EN_COURS),
        new Engin(
            "GRU-TER-02",
            "Grue mobile 40 t",
            r,
            6,
            "Terex",
            "AC 40",
            "DK-8002-BM",
            520_000,
            310_000_000,
            52,
            6900,
            0,
            Final.REFORME_DEMANDEE),
        new Engin(
            "MAL-MER-01",
            "Camion malaxeur 8 m³",
            r,
            7,
            "Mercedes-Benz",
            "Arocs 3240",
            "DK-6601-BN",
            210_000,
            82_000_000,
            22,
            3000,
            3,
            Final.EN_COURS),
        new Engin(
            "MAL-MAN-02",
            "Camion malaxeur 9 m³",
            r,
            7,
            "MAN",
            "TGS 33.400",
            "DK-6602-BN",
            215_000,
            84_000_000,
            10,
            1200,
            5,
            Final.ATTENTE_RECEPTION),
        new Engin(
            "CHA-CAT-01",
            "Chargeuse sur pneus 3,5 m³",
            r,
            8,
            "Caterpillar",
            "950 GC",
            "DK-7201-BP",
            370_000,
            158_000_000,
            32,
            4200,
            6,
            Final.EN_COURS),
        new Engin(
            "CHA-VOL-02",
            "Chargeuse sur pneus 3 m³",
            r,
            8,
            "Volvo",
            "L90H",
            "DK-7202-BP",
            340_000,
            142_000_000,
            38,
            5000,
            0,
            Final.DISPONIBLE),
        new Engin(
            "TRA-JCB-01",
            "Tractopelle",
            r,
            8,
            "JCB",
            "3CX",
            "DK-7310-BQ",
            120_000,
            45_000_000,
            20,
            2800,
            5,
            Final.EN_COURS),
        new Engin(
            "GRP-SDMO-01",
            "Groupe électrogène 100 kVA",
            o,
            9,
            "SDMO",
            "J110",
            null,
            45_000,
            18_500_000,
            28,
            5200,
            1,
            Final.EN_COURS),
        new Engin(
            "GRP-CAT-02",
            "Groupe électrogène 250 kVA",
            o,
            9,
            "Caterpillar",
            "DE250",
            null,
            75_000,
            39_000_000,
            18,
            3400,
            3,
            Final.EN_COURS),
        new Engin(
            "GRP-FGW-03",
            "Groupe électrogène 60 kVA",
            o,
            9,
            "FG Wilson",
            "P65",
            null,
            32_000,
            11_000_000,
            40,
            7100,
            0,
            Final.DISPONIBLE),
        new Engin(
            "PLQ-WAC-01",
            "Plaque vibrante réversible 400 kg",
            o,
            10,
            "Wacker Neuson",
            "DPU 6555",
            null,
            18_000,
            4_800_000,
            15,
            900,
            5,
            Final.EN_COURS),
        new Engin(
            "STA-LEI-01",
            "Station totale topographique",
            o,
            11,
            "Leica",
            "TS07",
            null,
            25_000,
            9_500_000,
            24,
            0,
            4,
            Final.EN_COURS),
        new Engin(
            "INF-PC-01",
            pc,
            inf,
            12,
            "Dell",
            "Latitude 5440",
            null,
            3_500,
            950_000,
            14,
            0,
            1,
            Final.EN_COURS),
        new Engin(
            "INF-PC-02",
            pc,
            inf,
            12,
            "HP",
            "ProBook 450 G10",
            null,
            3_500,
            890_000,
            10,
            0,
            3,
            Final.EN_COURS),
        new Engin(
            "INF-TAB-01",
            tablette,
            inf,
            13,
            "Samsung",
            "Galaxy Tab Active5",
            null,
            2_000,
            520_000,
            8,
            0,
            2,
            Final.EN_COURS),
        new Engin(
            "INF-TAB-02",
            tablette,
            inf,
            13,
            "Samsung",
            "Galaxy Tab Active5",
            null,
            2_000,
            520_000,
            8,
            0,
            0,
            Final.DISPONIBLE),
        new Engin(
            "CSM-ETA-01",
            "Lot de 100 étais métalliques",
            c,
            0,
            null,
            null,
            null,
            15_000,
            3_200_000,
            20,
            0,
            4,
            Final.EN_COURS),
        new Engin(
            "CSM-SIG-01",
            "Lot de signalisation temporaire (50 panneaux)",
            c,
            0,
            null,
            null,
            null,
            9_000,
            2_100_000,
            12,
            0,
            2,
            Final.EN_COURS),
        new Engin(
            "CSM-COF-01",
            "Lot de banches de coffrage (40 m²)",
            c,
            0,
            null,
            null,
            null,
            22_000,
            6_800_000,
            30,
            0,
            0,
            Final.DISPONIBLE));
  }

  /** Déroule chronologiquement la vie d'un engin sur les 9 derniers mois. */
  private final class Historique {

    private final int rang;
    private final Engin engin;
    private final Map<Integer, Chantier> chantiersParRang;
    private final TypeEngin type;
    private final LocalDate aujourdhui;
    private final int heuresParJour;
    private Materiel materiel;

    Historique(
        int rang,
        Engin engin,
        Map<Integer, Chantier> chantiersParRang,
        Map<Integer, TypeEngin> types,
        LocalDate aujourdhui) {
      this.rang = rang;
      this.engin = engin;
      this.chantiersParRang = chantiersParRang;
      this.type = engin.type() == 0 ? null : types.get(engin.type());
      this.aujourdhui = aujourdhui;
      boolean compteur = type != null && type.getDonnees().seuilMaintenanceHeures() != null;
      this.heuresParJour =
          !compteur ? 0 : engin.categorie() == CategorieMateriel.OUTILLAGE ? 4 : 5 + rang % 3;
    }

    void derouler() {
      LocalDate debut = aujourdhui.minusDays(265L - (rang % 10) * 3L);
      enregistrer(debut.minusDays(10));
      LocalDate fin0 = debut.plusDays(55L + (rang * 7L) % 30);
      cycle(chantier(rang % 7 + 1), debut, fin0, rang % 4 == 0 ? 0 : -1);
      LocalDate debut1 = fin0.plusDays(6);
      LocalDate fin1 = debut1.plusDays(50L + (rang * 11L) % 30);
      cycle(chantier((rang + 3) % 7 + 1), debut1, fin1, rang % 5 == 1 ? 1 : -1);
      etatFinal(fin1.plusDays(6));
      materiel = materiels.save(materiel);
    }

    private void enregistrer(LocalDate date) {
      DescriptionMateriel description =
          new DescriptionMateriel(
              engin.code(),
              engin.immatriculation(),
              engin.designation(),
              engin.categorie(),
              type == null ? null : type.getId(),
              engin.marque(),
              engin.modele(),
              BigDecimal.valueOf(engin.tarif()),
              BigDecimal.valueOf(engin.valeur()),
              aujourdhui.minusMonths(engin.ageMois()));
      materiel =
          materiels.save(
              Materiel.enregistrer(
                  idMateriel(rang),
                  description,
                  type,
                  engin.heures(),
                  Materiel.Origine.AUCUNE,
                  aujourdhui));
      materiel.localiser(DEPOT);
      tracer(TypeMouvement.ENREGISTREMENT, null, null, date, RM, "Entrée dans le parc");
      evenements.materiel(EventTypes.MATERIEL_ENREGISTRE, materiel, null, null);
    }

    /** Affectation passée complète : réception, éventuelle panne, entretien, libération. */
    private void cycle(Chantier chantier, LocalDate debut, LocalDate fin, int panne) {
      Affectation affectation =
          receptionner(affecter(chantier, debut, fin), chantier, debut.plusDays(1));
      cumulerHeures(debut, fin.minusDays(1));
      if (type != null) {
        Panne signalee = panne >= 0 ? signaler(fin.minusDays(2), GravitePanne.MAJEURE) : null;
        TypeMaintenance nature =
            signalee != null
                ? TypeMaintenance.CORRECTIVE
                : rang % 9 == 0 ? TypeMaintenance.LOURDE : TypeMaintenance.PREVENTIVE;
        Intervention ot = planifier(nature, fin.minusDays(2), signalee);
        cloturer(demarrer(ot, fin.minusDays(1)), fin);
      }
      affectation.terminer(fin, "Fin de mission sur " + chantier.getNom());
      materiel.liberer();
      affectation = affectations.save(affectation);
      evenements.affectation(EventTypes.AFFECTATION_TERMINEE, affectation, materiel, chantier);
      tracer(TypeMouvement.LIBERATION, chantier, null, fin, RM, "Retour au parc central");
      materiel.localiser(DEPOT);
    }

    private void etatFinal(LocalDate debutCourant) {
      Chantier courant = engin.chantier() == 0 ? null : chantier(engin.chantier());
      LocalDate finCourante = aujourdhui.plusDays(30L + (rang * 13L) % 150);
      switch (engin.etat()) {
        case EN_COURS -> enCours(courant, debutCourant, finCourante);
        case FIN_PROCHE -> enCours(courant, debutCourant, aujourdhui.plusDays(2));
        case ATTENTE_RECEPTION -> affecter(courant, aujourdhui, aujourdhui.plusDays(150));
        case DISPONIBLE ->
            evenements.materiel(EventTypes.MATERIEL_ETAT_CHANGE, materiel, null, "Au parc central");
        case MAINTENANCE_EN_COURS ->
            demarrer(
                planifier(TypeMaintenance.LOURDE, aujourdhui.minusDays(5), null),
                aujourdhui.minusDays(3));
        case MAINTENANCE_SUR_CHANTIER -> {
          enCours(courant, debutCourant, finCourante);
          Panne panne = signaler(aujourdhui.minusDays(4), GravitePanne.BLOQUANTE);
          demarrer(
              planifier(TypeMaintenance.CORRECTIVE, aujourdhui.minusDays(3), panne),
              aujourdhui.minusDays(2));
        }
        case MAINTENANCE_PLANIFIEE -> {
          enCours(courant, debutCourant, finCourante);
          planifier(TypeMaintenance.PREVENTIVE, aujourdhui.plusDays(6), null);
        }
        case PANNE_OUVERTE -> {
          enCours(courant, debutCourant, finCourante);
          signaler(aujourdhui.minusDays(1), GravitePanne.MAJEURE);
        }
        case REFORME -> {
          demanderReforme(aujourdhui.minusDays(40));
          materiel.deciderReforme(true, new ContexteMateriel(false, false));
          tracer(
              TypeMouvement.REFORME,
              null,
              null,
              aujourdhui.minusDays(35),
              DG,
              "Réforme validée par la Direction Générale — mise en vente aux enchères");
          evenements.materiel(
              EventTypes.MATERIEL_REFORME, materiel, null, materiel.getMotifReforme());
        }
        case REFORME_DEMANDEE -> demanderReforme(aujourdhui.minusDays(6));
      }
    }

    private void enCours(Chantier chantier, LocalDate debut, LocalDate fin) {
      receptionner(affecter(chantier, debut, fin), chantier, debut.plusDays(1));
      cumulerHeures(debut, aujourdhui);
    }

    private Affectation affecter(Chantier chantier, LocalDate debut, LocalDate fin) {
      Affectation affectation =
          affectations.save(
              PolitiqueAffectation.affecter(
                  materiel, chantier, null, new Periode(debut, fin), List.of()));
      materiel.localiser(proche(chantier.getCoordonnees()));
      evenements.affectation(EventTypes.MATERIEL_AFFECTE, affectation, materiel, chantier);
      tracer(
          TypeMouvement.AFFECTATION,
          null,
          chantier,
          debut,
          RM,
          "Affectation du " + debut + " au " + fin);
      return affectation;
    }

    // Chaque enregistrement renvoie l'agrégat à sa nouvelle version : c'est lui qui doit être
    // modifié ensuite (l'ancienne instance échouerait au verrou optimiste).
    private Affectation receptionner(Affectation affectation, Chantier chantier, LocalDate date) {
      affectation.confirmerReception(instant(date), "Reçu en bon état de marche");
      Affectation recue = affectations.save(affectation);
      evenements.affectation(EventTypes.RECEPTION_CONFIRMEE, recue, materiel, chantier);
      tracer(TypeMouvement.RECEPTION, null, chantier, date, chef(chantier), "Réception confirmée");
      return recue;
    }

    private Panne signaler(LocalDate date, GravitePanne gravite) {
      Chantier chantier =
          chantiersParRang.values().stream()
              .filter(c -> c.getId().equals(materiel.getChantierActuelId()))
              .findFirst()
              .orElse(null);
      Mouvement.Auteur auteur = chef(chantier);
      Panne panne =
          pannes.save(
              Panne.signaler(
                  materiel,
                  gravite,
                  PANNES[rang % PANNES.length],
                  auteur.id(),
                  auteur.nom(),
                  instant(date)));
      evenements.panneSignalee(panne, materiel, chantier);
      return panne;
    }

    private Intervention planifier(TypeMaintenance nature, LocalDate date, Panne panne) {
      String numero =
          String.format(
              "%s-%d-%04d",
              parametres.numerotation().prefixeOrdreTravail(),
              date.getYear(),
              sequences.prochaineValeur("ot", date.getYear()));
      Maintenance ot =
          Maintenance.planifier(
              numero,
              materiel,
              new Maintenance.Planification(
                  nature,
                  description(nature, panne),
                  date,
                  TECHNICIENS[rang % TECHNICIENS.length],
                  panne == null ? null : panne.getId()));
      if (panne != null) {
        panne.prendreEnCharge(ot.getId(), materiel.getId());
      }
      Maintenance enregistre = maintenances.save(ot);
      Panne priseEnCharge = panne == null ? null : pannes.save(panne);
      evenements.maintenance(EventTypes.MAINTENANCE_PLANIFIEE, enregistre, materiel);
      return new Intervention(enregistre, priseEnCharge);
    }

    private Intervention demarrer(Intervention intervention, LocalDate date) {
      Maintenance ot = intervention.ordre();
      ot.demarrer(date);
      materiel.mettreEnMaintenance();
      Maintenance enCours = maintenances.save(ot);
      evenements.maintenance(EventTypes.MAINTENANCE_DEMARREE, enCours, materiel);
      evenements.materiel(
          EventTypes.MATERIEL_ETAT_CHANGE,
          materiel,
          null,
          "Ordre de travail " + ot.getNumeroOt() + " démarré");
      tracer(
          TypeMouvement.MAINTENANCE,
          null,
          null,
          date,
          RM,
          "Début de l'ordre de travail " + ot.getNumeroOt());
      return new Intervention(enCours, intervention.panne());
    }

    // Les interventions passées ont lieu sur chantier : le matériel y retourne (AFFECTE).
    private void cloturer(Intervention intervention, LocalDate date) {
      Maintenance ot = intervention.ordre();
      ot.cloturer(
          date,
          cout(ot.getType()),
          "Intervention réalisée, essais concluants",
          materiel.getHeuresFonctionnement());
      materiel.enregistrerMaintenance(date);
      materiel.remettreEnService(true);
      Maintenance cloturee = maintenances.save(ot);
      if (intervention.panne() != null) {
        intervention.panne().resoudre();
        pannes.save(intervention.panne());
      }
      evenements.maintenance(EventTypes.MAINTENANCE_CLOTUREE, cloturee, materiel);
      evenements.materiel(
          EventTypes.MATERIEL_ETAT_CHANGE,
          materiel,
          null,
          "Ordre de travail " + ot.getNumeroOt() + " clôturé");
      tracer(
          TypeMouvement.MAINTENANCE,
          null,
          null,
          date,
          RM,
          "Fin de l'ordre de travail " + ot.getNumeroOt());
    }

    private void demanderReforme(LocalDate date) {
      String motif =
          "Usure générale avancée ("
              + materiel.getHeuresFonctionnement()
              + " h) : coût de remise en état supérieur à 60 % de la valeur à neuf";
      materiel.demanderReforme(motif);
      tracer(TypeMouvement.REFORME, null, null, date, RM, "Demande de réforme : " + motif);
      evenements.materiel(EventTypes.MATERIEL_REFORME_DEMANDEE, materiel, null, motif);
    }

    private void cumulerHeures(LocalDate du, LocalDate au) {
      long jours = Math.max(0, ChronoUnit.DAYS.between(du, au));
      materiel.releverHeures(materiel.getHeuresFonctionnement() + jours * heuresParJour);
    }

    private BigDecimal cout(TypeMaintenance nature) {
      long base =
          switch (nature) {
            case PREVENTIVE ->
                engin.categorie() == CategorieMateriel.MATERIEL_ROULANT
                    ? 450_000 + (rang * 37_000L) % 400_000
                    : 35_000 + (rang * 9_000L) % 90_000;
            case CORRECTIVE -> 900_000 + (rang * 113_000L) % 2_500_000;
            case LOURDE -> 7_500_000 + rang * 250_000L;
          };
      return BigDecimal.valueOf(base);
    }

    private String description(TypeMaintenance nature, Panne panne) {
      return switch (nature) {
        case PREVENTIVE -> "Entretien préventif : vidange, filtres, graissage et contrôle général";
        case CORRECTIVE ->
            "Réparation suite à panne : "
                + (panne == null ? "diagnostic atelier" : panne.getDescription());
        case LOURDE -> "Révision lourde : moteur et transmission";
      };
    }

    private void tracer(
        TypeMouvement nature,
        Chantier origine,
        Chantier destination,
        LocalDate date,
        Mouvement.Auteur auteur,
        String commentaire) {
      mouvements.ajouter(
          Mouvement.de(
              materiel.getId(), nature, origine, destination, instant(date), auteur, commentaire));
    }

    private Chantier chantier(int rangChantier) {
      return chantiersParRang.get(rangChantier);
    }

    private Coordonnees proche(Coordonnees centre) {
      return new Coordonnees(
          centre.latitude() + ((rang % 5) - 2) * 0.0015,
          centre.longitude() + ((rang % 3) - 1) * 0.0018);
    }
  }

  /**
   * Ordre de travail et panne associée, à leur dernière version enregistrée.
   *
   * @param ordre ordre de travail
   * @param panne panne prise en charge (peut être {@code null})
   */
  private record Intervention(Maintenance ordre, Panne panne) {}

  private static Mouvement.Auteur chef(Chantier chantier) {
    if (chantier == null || chantier.getChefChantierId() == null) {
      return RM;
    }
    return chantier.getChefChantierId().equals(CHEF_DIOP.id()) ? CHEF_DIOP : CHEF_NDIAYE;
  }

  private static DonneesChantier chantier(
      String code,
      String nom,
      String localisation,
      String region,
      double latitude,
      double longitude,
      long budget,
      long montantMarche,
      LocalDate debut,
      LocalDate finPrevue,
      UUID chef,
      String chefNom) {
    return new DonneesChantier(
        code,
        nom,
        localisation,
        region,
        new Coordonnees(latitude, longitude),
        BigDecimal.valueOf(budget),
        BigDecimal.valueOf(montantMarche),
        debut,
        finPrevue,
        chef,
        chefNom);
  }

  /**
   * Identifiant déterministe partagé avec le jeu de démonstration de ms-besoins.
   *
   * @param rang rang de l'engin dans {@link #engins()} (base 0)
   * @return {@code a1000000-0000-0000-0000-0000000000NN} avec NN = rang + 1
   */
  static UUID idMateriel(int rang) {
    return uuid(String.format("a1000000-0000-0000-0000-%012d", rang + 1));
  }

  private static Instant instant(LocalDate date) {
    return date.atTime(LocalTime.of(8, 30)).toInstant(ZoneOffset.UTC);
  }

  private static UUID uuid(String valeur) {
    return UUID.fromString(valeur);
  }
}
