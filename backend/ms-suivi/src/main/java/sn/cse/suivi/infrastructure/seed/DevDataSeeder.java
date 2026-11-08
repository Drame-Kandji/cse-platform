package sn.cse.suivi.infrastructure.seed;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
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
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.ChantierRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.application.port.out.TypeEnginRepository;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Coordonnees;
import sn.cse.suivi.domain.model.DescriptionMateriel;
import sn.cse.suivi.domain.model.DonneesChantier;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.Periode;
import sn.cse.suivi.domain.model.TypeEngin;
import sn.cse.suivi.domain.model.TypeMouvement;
import sn.cse.suivi.domain.service.PolitiqueAffectation;

/**
 * Jeu de données de démonstration (profil {@code dev}, docs/API_CONTRACT.md §12), volontairement
 * réduit : les chantiers en cours, les types d'engins et un parc de 15 matériels entrés dans le
 * parc le premier jour du mois courant. 6 sont affectés à un chantier depuis ce jour (réception
 * confirmée), les 9 autres sont disponibles au dépôt. Aucune panne ni maintenance : la
 * démonstration crée elle-même ses données. Idempotent : ne fait rien si le premier chantier existe
 * déjà. Passe par les agrégats du domaine et par l'outbox, afin que reporting, notification et
 * audit reçoivent de vrais événements.
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
          uuid("c0000000-0000-0000-0000-000000000006"));

  private static final Mouvement.Auteur RM =
      new Mouvement.Auteur(uuid("00000000-0000-0000-0000-000000000201"), "Ibrahima Fall");
  private static final Mouvement.Auteur CHEF_DIOP =
      new Mouvement.Auteur(uuid("00000000-0000-0000-0000-000000000101"), "Mamadou Diop");
  private static final Mouvement.Auteur CHEF_NDIAYE =
      new Mouvement.Auteur(uuid("00000000-0000-0000-0000-000000000102"), "Awa Ndiaye");

  /** Matériels affectés dès l'entrée dans le parc : code parc → code chantier. */
  static final Map<String, String> AFFECTATIONS =
      Map.of(
          "PEL-CAT320-02", "CH-DKR-01",
          "CAM-VOL-02", "CH-DKR-01",
          "BUL-KOM-01", "CH-THS-01",
          "GRU-LIE-01", "CH-STL-01",
          "NIV-CAT-01", "CH-KLK-01",
          "MAL-MER-01", "CH-KLK-01");

  /** Durée des affectations de départ, en mois. */
  private static final int DUREE_AFFECTATION_MOIS = 3;

  /** Parc central de Rufisque : position des matériels au dépôt. */
  private static final Coordonnees DEPOT = new Coordonnees(14.7236, -17.2733);

  /**
   * Fiche d'un engin de démonstration.
   *
   * @param code code parc
   * @param designation désignation
   * @param categorie catégorie
   * @param type rang du type d'engin (1..n)
   * @param marque marque
   * @param modele modèle
   * @param immatriculation immatriculation
   * @param tarif tarif journalier (FCFA)
   * @param valeur valeur d'acquisition (FCFA)
   * @param heures compteur à l'entrée dans le parc
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
      long heures) {}

  private final ChantierRepository chantiers;
  private final AffectationRepository affectations;
  private final TypeEnginRepository types;
  private final MaterielRepository materiels;
  private final MouvementRepository mouvements;
  private final SuiviEventPublisher evenements;
  private final Clock clock;
  private final TransactionTemplate transaction;

  /**
   * @param chantiers stockage des chantiers
   * @param affectations stockage des affectations
   * @param types stockage des types d'engins
   * @param materiels stockage des matériels
   * @param mouvements journal des mouvements
   * @param evenements publication des événements
   * @param clock horloge
   * @param transactions gestionnaire de transactions
   */
  public DevDataSeeder(
      ChantierRepository chantiers,
      AffectationRepository affectations,
      TypeEnginRepository types,
      MaterielRepository materiels,
      MouvementRepository mouvements,
      SuiviEventPublisher evenements,
      Clock clock,
      PlatformTransactionManager transactions) {
    this.chantiers = chantiers;
    this.affectations = affectations;
    this.types = types;
    this.materiels = materiels;
    this.mouvements = mouvements;
    this.evenements = evenements;
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
      LOG.debug("Premier chantier présent : jeu de démonstration non recréé");
      return false;
    }
    LocalDate aujourdhui = LocalDate.now(clock);
    LocalDate debutDuMois = aujourdhui.withDayOfMonth(1);
    Map<String, Chantier> parCode = creerChantiers(debutDuMois);
    Map<Integer, TypeEngin> parType = creerTypes();
    List<Engin> engins = engins();
    for (int i = 0; i < engins.size(); i++) {
      Materiel materiel = enregistrer(i, engins.get(i), parType, aujourdhui, debutDuMois);
      String chantier = AFFECTATIONS.get(materiel.getCode());
      if (chantier != null) {
        affecter(materiel, parCode.get(chantier), debutDuMois);
      }
    }
    return true;
  }

  private Map<String, Chantier> creerChantiers(LocalDate d) {
    UUID diop = CHEF_DIOP.id();
    UUID ndiaye = CHEF_NDIAYE.id();
    List<DonneesChantier> donnees =
        List.of(
            chantier(
                "CH-DKR-01",
                "Extension autoroute Dakar-AIBD",
                "Diamniadio",
                "Dakar",
                14.7167,
                -17.4677,
                18_500_000_000L,
                21_000_000_000L,
                d,
                d.plusMonths(18),
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
                d,
                d.plusMonths(12),
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
                d,
                d.plusMonths(14),
                ndiaye,
                CHEF_NDIAYE.nom()),
            chantier(
                "CH-KLK-01",
                "Réhabilitation route Kaolack-Fatick",
                "Kaolack",
                "Kaolack",
                14.1652,
                -16.0758,
                12_300_000_000L,
                13_900_000_000L,
                d,
                d.plusMonths(16),
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
                d,
                d.plusMonths(8),
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
                d,
                d.plusMonths(10),
                null,
                null));
    Map<String, Chantier> parCode = new LinkedHashMap<>();
    for (int i = 0; i < donnees.size(); i++) {
      Chantier c = chantiers.save(Chantier.ouvrir(CHANTIERS.get(i), donnees.get(i)));
      evenements.chantier(EventTypes.CHANTIER_CREE, c);
      parCode.put(c.getCode(), c);
    }
    return parCode;
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

  /**
   * Parc de démonstration. Aucun « compacteur vibrant » : un besoin de ce type part aux achats
   * (docs/DEMO.md), alors qu'un besoin de « compacteur » trouve le compacteur à pneus disponible.
   */
  private static List<Engin> engins() {
    CategorieMateriel r = CategorieMateriel.MATERIEL_ROULANT;
    CategorieMateriel o = CategorieMateriel.OUTILLAGE;
    CategorieMateriel inf = CategorieMateriel.EQUIPEMENT_INFORMATIQUE;
    String pelle20 = "Pelle hydraulique sur chenilles 20 t";
    String benne25 = "Camion benne 6x4 25 t";
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
            3200),
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
            1900),
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
            4600),
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
            4100),
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
            4300),
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
            4400),
        new Engin(
            "COM-HAM-01",
            "Compacteur à pneus 24 t",
            r,
            5,
            "Hamm",
            "GRW 280",
            "DK-4412-BJ",
            170_000,
            61_000_000,
            6100),
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
            4200),
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
            5200),
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
            900),
        new Engin(
            "INF-PC-01",
            "Ordinateur portable bureau de chantier",
            inf,
            12,
            "Dell",
            "Latitude 5440",
            null,
            3_500,
            950_000,
            0),
        new Engin(
            "INF-TAB-01",
            "Tablette de chantier durcie",
            inf,
            13,
            "Samsung",
            "Galaxy Tab Active5",
            null,
            2_000,
            520_000,
            0),
        new Engin(
            "GRU-LIE-01",
            "Grue mobile tout-terrain 50 t",
            r,
            6,
            "Liebherr",
            "LTM 1050-3.1",
            "DK-8101-BR",
            520_000,
            310_000_000,
            2700),
        new Engin(
            "MAL-MER-01",
            "Camion malaxeur 8 m³",
            r,
            7,
            "Mercedes-Benz",
            "Arocs 3340",
            "DK-6604-BL",
            160_000,
            72_000_000,
            3500),
        new Engin(
            "TOP-LEI-01",
            "Station totale topographique",
            o,
            11,
            "Leica",
            "TS07",
            null,
            12_000,
            9_500_000,
            0));
  }

  private Materiel enregistrer(
      int rang,
      Engin engin,
      Map<Integer, TypeEngin> types,
      LocalDate aujourdhui,
      LocalDate entree) {
    TypeEngin type = types.get(engin.type());
    DescriptionMateriel description =
        new DescriptionMateriel(
            engin.code(),
            engin.immatriculation(),
            engin.designation(),
            engin.categorie(),
            type.getId(),
            engin.marque(),
            engin.modele(),
            BigDecimal.valueOf(engin.tarif()),
            BigDecimal.valueOf(engin.valeur()),
            entree);
    Materiel materiel =
        Materiel.enregistrer(
            idMateriel(rang),
            description,
            type,
            engin.heures(),
            Materiel.Origine.AUCUNE,
            aujourdhui);
    materiel.localiser(DEPOT);
    materiel = materiels.save(materiel);
    mouvements.ajouter(
        Mouvement.de(
            materiel.getId(),
            TypeMouvement.ENREGISTREMENT,
            null,
            null,
            entree.atTime(LocalTime.of(8, 30)).toInstant(ZoneOffset.UTC),
            RM,
            "Entrée dans le parc"));
    evenements.materiel(EventTypes.MATERIEL_ENREGISTRE, materiel, null, null);
    return materiel;
  }

  /** Affecte le matériel au chantier le jour de son entrée, réception confirmée par le chef. */
  private void affecter(Materiel materiel, Chantier chantier, LocalDate debut) {
    Periode periode = new Periode(debut, debut.plusMonths(DUREE_AFFECTATION_MOIS).minusDays(1));
    Affectation affectation =
        PolitiqueAffectation.affecter(materiel, chantier, null, periode, List.of());
    affectation = affectations.save(affectation);
    materiel.localiser(chantier.getCoordonnees());
    Materiel affecte = materiels.save(materiel);
    evenements.affectation(EventTypes.MATERIEL_AFFECTE, affectation, affecte, chantier);
    mouvements.ajouter(
        Mouvement.de(
            affecte.getId(),
            TypeMouvement.AFFECTATION,
            null,
            chantier,
            instant(debut, 10),
            RM,
            "Affectation du " + periode.debut() + " au " + periode.fin()));
    // Chaque enregistrement renvoie l'agrégat à sa nouvelle version (verrou optimiste).
    affectation.confirmerReception(instant(debut, 15), "Reçu en bon état de marche");
    Affectation recue = affectations.save(affectation);
    evenements.affectation(EventTypes.RECEPTION_CONFIRMEE, recue, affecte, chantier);
    mouvements.ajouter(
        Mouvement.de(
            affecte.getId(),
            TypeMouvement.RECEPTION,
            null,
            chantier,
            instant(debut, 15),
            chef(chantier),
            "Réception confirmée"));
  }

  private static Mouvement.Auteur chef(Chantier chantier) {
    return CHEF_DIOP.id().equals(chantier.getChefChantierId()) ? CHEF_DIOP : CHEF_NDIAYE;
  }

  private static Instant instant(LocalDate date, int heure) {
    return date.atTime(LocalTime.of(heure, 0)).toInstant(ZoneOffset.UTC);
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
   * Identifiant déterministe d'un matériel de démonstration.
   *
   * @param rang rang de l'engin dans {@link #engins()} (base 0)
   * @return {@code a1000000-0000-0000-0000-0000000000NN} avec NN = rang + 1
   */
  static UUID idMateriel(int rang) {
    return uuid(String.format("a1000000-0000-0000-0000-%012d", rang + 1));
  }

  private static UUID uuid(String valeur) {
    return UUID.fromString(valeur);
  }
}
