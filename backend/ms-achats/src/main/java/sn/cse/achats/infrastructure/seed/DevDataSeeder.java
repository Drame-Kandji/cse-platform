package sn.cse.achats.infrastructure.seed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.BesoinAAcheterRepository;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.application.port.out.FournisseurProjectionRepository;
import sn.cse.achats.application.port.out.ReferenceGenerator;
import sn.cse.achats.application.service.ParametresAchats;
import sn.cse.achats.domain.model.Acteur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.BesoinAAcheter;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.ClausesContrat;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.CriterePondere;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.model.FournisseurConnu;
import sn.cse.achats.domain.model.NouveauJalon;
import sn.cse.achats.domain.model.NouvelAppelOffre;
import sn.cse.achats.domain.model.PonderationCriteres;
import sn.cse.achats.domain.model.Priorite;
import sn.cse.achats.domain.model.StatutFacture;
import sn.cse.achats.domain.model.StatutFournisseur;
import sn.cse.achats.domain.model.TypeAppelOffre;
import sn.cse.events.EventTypes;

/**
 * Historique d'achats de démonstration (profil {@code dev}) sur les neuf derniers mois, relatif à
 * l'horloge : 13 appels d'offres dans tous les statuts (dont 9 issus des besoins EN_ACHAT de
 * ms-besoins), 9 contrats couvrant tout le cycle de vie (signature en deux temps CSE puis
 * fournisseur, expéditions déclarées sur le portail), engagements, livraisons à l'heure et en
 * retard, 11 factures. Le fournisseur 1 (compte portail de démonstration) dispose d'un appel
 * d'offres ouvert sans offre, d'un contrat à signer, d'un contrat prêt à expédier, d'un contrat
 * livré à facturer et de factures RECUE, MANDATEE et PAYEE. Tout passe par les agrégats et l'outbox
 * afin que reporting, notification et audit reçoivent de vrais événements. Idempotent : ne fait
 * rien si des appels d'offres existent déjà. Identifiants partagés : API_CONTRACT §12.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "cse.seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);

  static final Acteur RA = new Acteur(id("00000000-0000-0000-0000-000000000301"), "Fatou Sarr");
  static final Acteur RM = new Acteur(id("00000000-0000-0000-0000-000000000201"), "Ibrahima Fall");
  static final Acteur DAF = new Acteur(id("00000000-0000-0000-0000-000000000401"), "Ousmane Ba");
  static final Acteur DG = new Acteur(id("00000000-0000-0000-0000-000000000501"), "Aminata Sow");

  /** Compte portail de démonstration du fournisseur 1 (fournisseur@cse.sn). */
  static final Acteur PORTAIL_SAHEL =
      new Acteur(id("00000000-0000-0000-0000-000000000701"), "Moussa Kane");

  static final FournisseurConnu SAHEL = fournisseur(1, "Sahel Engins SA", StatutFournisseur.ACTIF);
  static final FournisseurConnu TERANGA =
      fournisseur(2, "Teranga Équipements BTP", StatutFournisseur.ACTIF);
  static final FournisseurConnu DAKAR_OUTILLAGE =
      fournisseur(3, "Dakar Outillage Pro", StatutFournisseur.ACTIF);
  static final FournisseurConnu CAP_VERT =
      fournisseur(4, "Cap-Vert Informatique", StatutFournisseur.ACTIF);
  static final FournisseurConnu ATLANTIQUE =
      fournisseur(5, "Atlantique Consommables SARL", StatutFournisseur.ACTIF);
  static final FournisseurConnu NDIAMBOUR =
      fournisseur(6, "Ndiambour Matériaux", StatutFournisseur.SUSPENDU);
  static final FournisseurConnu BAOBAB =
      fournisseur(7, "Baobab Logistique", StatutFournisseur.EN_ATTENTE_VALIDATION);

  /**
   * Besoins EN_ACHAT du jeu de démonstration de ms-besoins ({@code JeuDeDonneesBesoins}, numéros 13
   * à 23) : données identiques pour que l'histoire soit la même dans les deux services.
   */
  private record BesoinSeed(
      int numero,
      int chantier,
      String designation,
      CategorieMateriel categorie,
      int quantite,
      Priorite priorite,
      int joursAvant,
      int heuresDecision,
      String specifications) {}

  private static final List<BesoinSeed> BESOINS_EN_ACHAT =
      List.of(
          new BesoinSeed(
              13,
              1,
              "Grue mobile 50 t",
              CategorieMateriel.MATERIEL_ROULANT,
              1,
              Priorite.CRITIQUE,
              262,
              3,
              "Flèche télescopique 40 m"),
          new BesoinSeed(
              14,
              3,
              "Barge de travaux 200 t",
              CategorieMateriel.MATERIEL_ROULANT,
              1,
              Priorite.URGENT,
              233,
              12,
              "Avec pousseur"),
          new BesoinSeed(
              15,
              2,
              "Finisseur d'enrobés",
              CategorieMateriel.MATERIEL_ROULANT,
              1,
              Priorite.NORMAL,
              210,
              48,
              "Largeur 2,5 à 8 m"),
          new BesoinSeed(
              16,
              4,
              "Camion malaxeur 8 m³",
              CategorieMateriel.MATERIEL_ROULANT,
              2,
              Priorite.URGENT,
              186,
              9,
              null),
          new BesoinSeed(
              17,
              6,
              "Bulldozer D6",
              CategorieMateriel.MATERIEL_ROULANT,
              1,
              Priorite.NORMAL,
              160,
              30,
              null),
          new BesoinSeed(
              18,
              5,
              "Ordinateur portable de chantier",
              CategorieMateriel.EQUIPEMENT_INFORMATIQUE,
              4,
              Priorite.NORMAL,
              140,
              52,
              "16 Go RAM, suite bureautique"),
          new BesoinSeed(
              19,
              1,
              "Porte-char 50 t",
              CategorieMateriel.MATERIEL_ROULANT,
              1,
              Priorite.URGENT,
              118,
              7,
              null),
          new BesoinSeed(
              20,
              2,
              "Citerne à eau 10 000 L",
              CategorieMateriel.MATERIEL_ROULANT,
              2,
              Priorite.NORMAL,
              95,
              44,
              "Rampe d'arrosage"),
          new BesoinSeed(
              21,
              6,
              "GPS RTK",
              CategorieMateriel.EQUIPEMENT_INFORMATIQUE,
              2,
              Priorite.NORMAL,
              70,
              25,
              null),
          new BesoinSeed(
              22,
              3,
              "Gilets de sauvetage (EPI)",
              CategorieMateriel.CONSOMMABLE,
              40,
              Priorite.CRITIQUE,
              52,
              2,
              "Norme EN ISO 12402"),
          new BesoinSeed(
              23,
              4,
              "Disques diamant 350 mm",
              CategorieMateriel.CONSOMMABLE,
              30,
              Priorite.NORMAL,
              33,
              20,
              "Pour scie de sol"));

  /**
   * Ancienneté de soumission (jours) des 40 besoins de ms-besoins, par numéro : sert à reproduire
   * leurs références (attribuées dans l'ordre chronologique de soumission).
   */
  private static final Map<Integer, Integer> SOUMISSIONS_BESOINS =
      Map.ofEntries(
          Map.entry(1, 268),
          Map.entry(2, 255),
          Map.entry(3, 240),
          Map.entry(4, 221),
          Map.entry(5, 205),
          Map.entry(6, 190),
          Map.entry(7, 172),
          Map.entry(8, 150),
          Map.entry(9, 128),
          Map.entry(10, 101),
          Map.entry(11, 76),
          Map.entry(12, 45),
          Map.entry(13, 262),
          Map.entry(14, 233),
          Map.entry(15, 210),
          Map.entry(16, 186),
          Map.entry(17, 160),
          Map.entry(18, 140),
          Map.entry(19, 118),
          Map.entry(20, 95),
          Map.entry(21, 70),
          Map.entry(22, 52),
          Map.entry(23, 33),
          Map.entry(24, 245),
          Map.entry(25, 252),
          Map.entry(26, 175),
          Map.entry(27, 130),
          Map.entry(28, 88),
          Map.entry(29, 40),
          Map.entry(30, 6),
          Map.entry(31, 4),
          Map.entry(32, 15),
          Map.entry(33, 9),
          Map.entry(34, 3),
          Map.entry(35, 7),
          Map.entry(36, 12),
          Map.entry(37, 2),
          Map.entry(38, 1),
          Map.entry(39, 0),
          Map.entry(40, 20));

  /** Noms des chantiers tels que portés par les besoins de ms-besoins. */
  private static final Map<Integer, String> CHANTIERS_BESOINS =
      Map.of(
          1, "Extension autoroute Dakar-AIBD",
          2, "Contournement de Thiès RN2",
          3, "Pont de la lagune",
          4, "Réhabilitation route Kaolack-Fatick",
          5, "Bâtiment administratif",
          6, "Aménagement hydraulique");

  private static final BigDecimal TVA = new BigDecimal("0.18");

  private final AppelOffreRepository appelsOffres;
  private final ContratRepository contrats;
  private final FactureRepository factures;
  private final BesoinAAcheterRepository besoins;
  private final FournisseurProjectionRepository fournisseurs;
  private final ReferenceGenerator references;
  private final AchatsEventPublisher evenements;
  private final ParametresAchats parametres;
  private final TransactionTemplate transaction;
  private final Clock clock;

  /**
   * @param appelsOffres dépôt des appels d'offres
   * @param contrats dépôt des contrats
   * @param factures dépôt des factures
   * @param besoins projection des besoins
   * @param fournisseurs projection des fournisseurs
   * @param references références
   * @param evenements outbox
   * @param parametres paramètres métier
   * @param transactions gestionnaire de transactions
   * @param clock horloge
   */
  public DevDataSeeder(
      AppelOffreRepository appelsOffres,
      ContratRepository contrats,
      FactureRepository factures,
      BesoinAAcheterRepository besoins,
      FournisseurProjectionRepository fournisseurs,
      ReferenceGenerator references,
      AchatsEventPublisher evenements,
      ParametresAchats parametres,
      PlatformTransactionManager transactions,
      Clock clock) {
    this.appelsOffres = appelsOffres;
    this.contrats = contrats;
    this.factures = factures;
    this.besoins = besoins;
    this.fournisseurs = fournisseurs;
    this.references = references;
    this.evenements = evenements;
    this.parametres = parametres;
    this.transaction = new TransactionTemplate(transactions);
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (appelsOffres.compter() > 0) {
      LOG.info("Jeu de démonstration ms-achats déjà présent : rien à faire");
      return;
    }
    transaction.executeWithoutResult(s -> semer());
    LOG.info("Jeu de démonstration ms-achats créé (13 appels d'offres, 9 contrats, 11 factures)");
  }

  private void semer() {
    // Statuts initiaux, écrasés par tout événement réel de ms-fournisseurs (date plus récente).
    for (FournisseurConnu f :
        List.of(SAHEL, TERANGA, DAKAR_OUTILLAGE, CAP_VERT, ATLANTIQUE, NDIAMBOUR, BAOBAB)) {
      fournisseurs.enregistrerSiPlusRecent(f, Instant.EPOCH);
    }
    // Besoins orientés vers l'achat par le seed de ms-besoins (numéros 13 à 23, mêmes données) :
    // les événements BESOIN_EN_ACHAT réels écraseront ces projections sans effet de bord.
    for (BesoinSeed b : BESOINS_EN_ACHAT) {
      besoins.enregistrer(projection(b));
    }

    // AO-001 : grue mobile (besoin 13), marché > seuil (DG), contrat CLOTURE livré à l'heure.
    AppelOffre ao1 =
        publier(
            255,
            20,
            "Acquisition d'une grue mobile 50 t",
            TypeAppelOffre.APPEL_OFFRES,
            "320000000",
            13,
            criteres(
                CritereEvaluation.PRIX,
                40,
                CritereEvaluation.DELAI,
                20,
                CritereEvaluation.QUALITE_TECHNIQUE,
                25,
                CritereEvaluation.SAV,
                15),
            List.of(SAHEL, TERANGA));
    ao1 = offre(ao1, SAHEL, "298000000", 60, 24, "Liebherr LTM 1050-3.1, formation incluse", 248);
    ao1 = offre(ao1, TERANGA, "312000000", 45, 18, "Grove GMK3060L", 242);
    ao1 = cloturer(ao1);
    ao1 =
        noter(
            ao1, SAHEL, Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 90, CritereEvaluation.SAV, 85));
    ao1 =
        noter(
            ao1,
            TERANGA,
            Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 80, CritereEvaluation.SAV, 75));
    ao1 = attribuer(ao1, SAHEL, "Meilleur score global, réseau SAV à Dakar.", 230, 228, 226);
    ContratAchat c1 = contrat(ao1, 225, "24", 60, "Livraison franco sur le chantier de Diamniadio");
    c1 = signer(c1, 215);
    c1 = engagement(c1, true, "Crédits disponibles sur la ligne investissements 2026", 213);
    c1 = expedier(c1, 165, "Convoi exceptionnel depuis le port autonome de Dakar");
    c1 = jalons(c1, 208, 165);
    c1 = livrer(c1, 158, "BL-SE-2026-0112");
    Facture f = facture(c1, "FA-SE-2026-041", "120000000", 156);
    f = mandater(f, "MDT-2026-0031", 140);
    payer(f, 130);
    f = facture(c1, "FA-SE-2026-057", "132000000", 150);
    f = mandater(f, "MDT-2026-0044", 125);
    payer(f, 115);
    cloturerContrat(c1);

    // AO-002 : barge de travaux (besoin 14), DG, livraison en retard de 12 jours (pénalités).
    AppelOffre ao2 =
        publier(
            230,
            21,
            "Acquisition d'une barge de travaux 200 t avec pousseur",
            TypeAppelOffre.APPEL_OFFRES,
            "175000000",
            14,
            criteres(
                CritereEvaluation.PRIX,
                50,
                CritereEvaluation.DELAI,
                30,
                CritereEvaluation.GARANTIE,
                20),
            List.of(SAHEL, TERANGA));
    ao2 = offre(ao2, SAHEL, "168000000", 45, 24, "Barge modulaire Damen et pousseur 300 CV", 222);
    ao2 = offre(ao2, TERANGA, "159000000", 50, 18, "Barge Flexifloat S-70", 216);
    ao2 = cloturer(ao2);
    ao2 = noter(ao2, SAHEL, Map.of(CritereEvaluation.GARANTIE, 85));
    ao2 = noter(ao2, TERANGA, Map.of(CritereEvaluation.GARANTIE, 70));
    ao2 =
        attribuer(
            ao2, SAHEL, "Délai et garantie supérieurs malgré un prix plus élevé.", 202, 200, 197);
    ContratAchat c2 =
        contrat(ao2, 196, "24", 50, "Mise à l'eau sur le chantier du pont de la lagune");
    c2 = signer(c2, 190);
    c2 = engagement(c2, true, null, 188);
    c2 = expedier(c2, 130, "Barge remorquée depuis Dakar, pousseur par voie routière");
    c2 = jalons(c2, 180, 130);
    c2 = livrer(c2, 128, "BL-SE-2026-0187");
    f = facture(c2, "FA-SE-2026-079", "80000000", 126);
    f = mandater(f, "MDT-2026-0058", 110);
    payer(f, 100);
    f = facture(c2, "FA-SE-2026-102", "50000000", 20);
    mandater(f, "MDT-2026-0131", 12);
    facture(c2, "FA-SE-2026-133", "10000000", 5);

    // AO-003 : demande de prix groupes électrogènes (sans besoin), sous le seuil, livré à l'heure
    // (livraison démarrée par le RA, hors portail).
    AppelOffre ao3 =
        publier(
            205,
            14,
            "Fourniture de quatre groupes électrogènes 100 kVA",
            TypeAppelOffre.DEMANDE_PRIX,
            CategorieMateriel.OUTILLAGE,
            4,
            "48000000",
            null,
            criteres(CritereEvaluation.PRIX, 60, CritereEvaluation.DELAI, 40),
            List.of(TERANGA, DAKAR_OUTILLAGE));
    ao3 = offre(ao3, TERANGA, "46000000", 30, 12, "SDMO J110", 200);
    ao3 = offre(ao3, DAKAR_OUTILLAGE, "42500000", 35, 12, "Pramac GSW110", 196);
    ao3 = cloturer(ao3);
    ao3 = attribuer(ao3, TERANGA, "Classée première au comparatif (délai).", 186, 184, 0);
    ContratAchat c3 = contrat(ao3, 182, "12", 35, "Livraison au parc matériel de Dakar");
    c3 = signer(c3, 170);
    c3 = engagement(c3, true, null, 169);
    c3 = demarrer(c3);
    c3 = jalons(c3, 160, 141);
    c3 = livrer(c3, 140, "BL-TEB-2026-0045");
    f = facture(c3, "TEB-FAC-2026-0211", "30000000", 138);
    f = mandater(f, "MDT-2026-0072", 128);
    payer(f, 118);
    f = facture(c3, "TEB-FAC-2026-0230", "7000000", 100);
    rejeter(f, "Facture en doublon des frais de transport déjà inclus.");
    facture(c3, "TEB-FAC-2026-0298", "5500000", 70);

    // AO-004 : portables de chantier (besoin 18), livraison en retard en cours (alerte), démarrée
    // hors portail.
    AppelOffre ao4 =
        publier(
            120,
            15,
            "Fourniture de quatre ordinateurs portables de chantier",
            TypeAppelOffre.DEMANDE_PRIX,
            "6500000",
            18,
            criteres(
                CritereEvaluation.PRIX,
                50,
                CritereEvaluation.DELAI,
                20,
                CritereEvaluation.GARANTIE,
                30),
            List.of(CAP_VERT));
    ao4 = offre(ao4, CAP_VERT, "6200000", 30, 36, "4 Dell Latitude 5430 Rugged, 16 Go", 112);
    ao4 = cloturer(ao4);
    ao4 = noter(ao4, CAP_VERT, Map.of(CritereEvaluation.GARANTIE, 90));
    ao4 = attribuer(ao4, CAP_VERT, "Seule offre reçue, conforme au cahier des charges.", 95, 93, 0);
    ContratAchat c4 = contrat(ao4, 90, "36", 30, "Livraison et installation à Ziguinchor");
    c4 = signer(c4, 75);
    c4 = engagement(c4, true, null, 73);
    c4 = demarrer(c4);
    c4 = jalons(c4, 70, -1);
    f = facture(c4, "CVI-2026-0088", "2000000", 25);
    mandater(f, "MDT-2026-0102", 10);
    facture(c4, "CVI-2026-0119", "2500000", 50);
    retard(c4);

    // AO-005 : finisseur d'enrobés (besoin 15), DG, engagement validé : prêt à expédier (portail).
    AppelOffre ao5 =
        publier(
            100,
            21,
            "Acquisition d'un finisseur d'enrobés",
            TypeAppelOffre.APPEL_OFFRES,
            "200000000",
            15,
            criteres(
                CritereEvaluation.PRIX,
                45,
                CritereEvaluation.DELAI,
                25,
                CritereEvaluation.QUALITE_TECHNIQUE,
                30),
            List.of(SAHEL, TERANGA));
    ao5 = offre(ao5, SAHEL, "185000000", 75, 24, "Vögele Super 1800-3i", 90);
    ao5 = offre(ao5, TERANGA, "192000000", 60, 24, "Dynapac SD2550CS", 85);
    ao5 = cloturer(ao5);
    ao5 = noter(ao5, SAHEL, Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 92));
    ao5 = noter(ao5, TERANGA, Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 80));
    ao5 = attribuer(ao5, SAHEL, "Meilleur score technique, prix inférieur.", 70, 68, 66);
    ContratAchat c5 =
        contrat(ao5, 64, "24", 75, "Livraison sur le chantier du contournement de Thiès");
    c5 = signer(c5, 35);
    c5 = engagement(c5, true, "Engagement imputé au budget du chantier de Thiès", 33);
    rejeter(facture(c5, "FA-SE-2026-176", "10000000", 20), "Avance non prévue au contrat.");

    // AO-006 : gilets de sauvetage (besoin 22), contrat signé, engagement en attente de la DAF.
    AppelOffre ao6 =
        publier(
            45,
            10,
            "Fourniture de 40 gilets de sauvetage (EPI)",
            TypeAppelOffre.DEMANDE_PRIX,
            "1500000",
            22,
            criteres(CritereEvaluation.PRIX, 70, CritereEvaluation.DELAI, 30),
            List.of(ATLANTIQUE));
    ao6 = offre(ao6, ATLANTIQUE, "1400000", 10, null, "40 gilets 150 N certifiés EN ISO 12402", 40);
    ao6 = cloturer(ao6);
    ao6 = attribuer(ao6, ATLANTIQUE, "Prix conforme au catalogue négocié.", 30, 28, 0);
    ContratAchat c6 = contrat(ao6, 27, null, 10, "Livraison au chantier du pont de la lagune");
    signer(c6, 12);

    // AO-007 : camions malaxeurs (besoin 16), sous le seuil. Le fournisseur a refusé une première
    // version (pénalités) ; le RA a revu la clause et re-signé : en attente de signature (portail).
    AppelOffre ao7 =
        publier(
            70,
            18,
            "Acquisition de deux camions malaxeurs 8 m³",
            TypeAppelOffre.APPEL_OFFRES,
            "95000000",
            16,
            criteres(CritereEvaluation.PRIX, 60, CritereEvaluation.DELAI, 40),
            List.of(SAHEL, TERANGA));
    ao7 = offre(ao7, SAHEL, "89000000", 30, 12, "Deux Mercedes Arocs, toupie Liebherr 8 m³", 60);
    ao7 = offre(ao7, TERANGA, "84500000", 40, 12, "Deux Renault K 380, toupie Stetter", 58);
    ao7 = cloturer(ao7);
    ao7 = attribuer(ao7, SAHEL, "Délai de livraison décisif pour la saison sèche.", 40, 38, 0);
    ContratAchat c7 = contrat(ao7, 37, "12", 30, "Livraison au chantier Kaolack-Fatick");
    c7 = envoyerEnSignature(c7, 8);
    c7 =
        refuserSignature(
            c7,
            "Le taux de pénalité de 0,1 % par jour dépasse nos conditions générales ; nous"
                + " proposons 0,05 %.");
    c7.modifierClauses(
        new ClausesContrat(
            c7.getClauses().garantieMois(),
            c7.getClauses().conditionsMaintenance(),
            c7.getClauses().conditionsLivraison(),
            new BigDecimal("0.05")),
        c7.getDelaiLivraisonJours(),
        List.of());
    envoyerEnSignature(c7, 3);

    // AO-008 : outillage (sans besoin) attribué, contrat BROUILLON à rédiger.
    AppelOffre ao8 =
        publier(
            60,
            14,
            "Fourniture de six bétonnières 350 L et vibreurs",
            TypeAppelOffre.DEMANDE_PRIX,
            CategorieMateriel.OUTILLAGE,
            6,
            "25000000",
            null,
            criteres(
                CritereEvaluation.PRIX,
                60,
                CritereEvaluation.DELAI,
                20,
                CritereEvaluation.GARANTIE,
                20),
            List.of(TERANGA, DAKAR_OUTILLAGE));
    ao8 = offre(ao8, TERANGA, "23400000", 20, 12, "Bétonnières Altrad", 55);
    ao8 =
        offre(ao8, DAKAR_OUTILLAGE, "21600000", 25, 24, "Bétonnières Imer et vibreurs Wacker", 52);
    ao8 = cloturer(ao8);
    ao8 = noter(ao8, TERANGA, Map.of(CritereEvaluation.GARANTIE, 70));
    ao8 = noter(ao8, DAKAR_OUTILLAGE, Map.of(CritereEvaluation.GARANTIE, 80));
    ao8 = attribuer(ao8, DAKAR_OUTILLAGE, "Meilleur prix et garantie de 24 mois.", 10, 8, 0);
    contrat(ao8, 8, null, 25, null);

    // AO-009 : bulldozer D6 (besoin 17), > seuil : RM a co-validé, approbation DG en attente.
    AppelOffre ao9 =
        publier(
            45,
            21,
            "Acquisition d'un bulldozer D6",
            TypeAppelOffre.APPEL_OFFRES,
            "185000000",
            17,
            criteres(
                CritereEvaluation.PRIX,
                40,
                CritereEvaluation.DELAI,
                20,
                CritereEvaluation.QUALITE_TECHNIQUE,
                25,
                CritereEvaluation.SAV,
                15),
            List.of(SAHEL, TERANGA));
    ao9 = offre(ao9, SAHEL, "176000000", 90, 24, "CAT D6 XE", 35);
    ao9 = offre(ao9, TERANGA, "169000000", 100, 18, "Komatsu D65EX-18", 30);
    ao9 = cloturer(ao9);
    ao9 =
        noter(
            ao9, SAHEL, Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 85, CritereEvaluation.SAV, 90));
    ao9 =
        noter(
            ao9,
            TERANGA,
            Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 82, CritereEvaluation.SAV, 80));
    ao9 = proposer(ao9, SAHEL, "Meilleur score global ; SAV certifié Caterpillar à Dakar.", 5);
    ao9.coValiderAttribution(true, "Conforme au besoin du chantier de Touba.", RM, ilYA(3, 11));
    appelsOffres.enregistrer(ao9);

    // AO-010 : citernes à eau (besoin 20), sous le seuil : co-validation RM en attente.
    AppelOffre ao10 =
        publier(
            35,
            21,
            "Fourniture de deux citernes à eau 10 000 L",
            TypeAppelOffre.APPEL_OFFRES,
            "100000000",
            20,
            criteres(
                CritereEvaluation.PRIX,
                50,
                CritereEvaluation.DELAI,
                30,
                CritereEvaluation.GARANTIE,
                20),
            List.of(SAHEL, TERANGA));
    ao10 =
        offre(ao10, SAHEL, "96000000", 30, 24, "Deux porteurs Mercedes Actros, citerne inox", 25);
    ao10 = offre(ao10, TERANGA, "91500000", 35, 18, "Deux Renault C 430, citerne acier", 20);
    ao10 = cloturer(ao10);
    ao10 = noter(ao10, SAHEL, Map.of(CritereEvaluation.GARANTIE, 75));
    ao10 = noter(ao10, TERANGA, Map.of(CritereEvaluation.GARANTIE, 70));
    proposer(ao10, SAHEL, "Meilleur score global, délai compatible avec le planning.", 2);

    // AO-011 : stations totales, clôturé et évalué, attribution non encore proposée.
    AppelOffre ao11 =
        publier(
            25,
            14,
            "Fourniture de quatre stations totales GNSS",
            TypeAppelOffre.APPEL_OFFRES,
            CategorieMateriel.EQUIPEMENT_INFORMATIQUE,
            4,
            "30000000",
            null,
            criteres(
                CritereEvaluation.PRIX,
                50,
                CritereEvaluation.QUALITE_TECHNIQUE,
                30,
                CritereEvaluation.REFERENCES,
                20),
            List.of());
    ao11 = offre(ao11, CAP_VERT, "27400000", 21, 24, "Leica TS16 et GS18", 18);
    ao11 = offre(ao11, DAKAR_OUTILLAGE, "29900000", 28, 12, "Trimble S7", 15);
    ao11 = cloturer(ao11);
    ao11 =
        noter(
            ao11,
            CAP_VERT,
            Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 85, CritereEvaluation.REFERENCES, 90));
    noter(
        ao11,
        DAKAR_OUTILLAGE,
        Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 70, CritereEvaluation.REFERENCES, 60));

    // AO-012 : porte-char (besoin 19), publié (date limite dans 17 jours), offre anonymisée (R10).
    // Sahel Engins (invité) n'a pas encore soumissionné : dépôt possible depuis le portail.
    AppelOffre ao12 =
        publier(
            8,
            25,
            "Acquisition d'un porte-char 50 t",
            TypeAppelOffre.APPEL_OFFRES,
            "110000000",
            19,
            criteres(
                CritereEvaluation.PRIX,
                40,
                CritereEvaluation.DELAI,
                25,
                CritereEvaluation.QUALITE_TECHNIQUE,
                20,
                CritereEvaluation.SAV,
                15),
            List.of(SAHEL, TERANGA));
    offre(ao12, TERANGA, "102000000", 60, 24, "Tracteur Mercedes Actros et semi Goldhofer", 3);

    // AO-013 : chargeuses (sans besoin), sous le seuil, expédiées par le fournisseur sur le
    // portail et livrées à l'heure ; aucune facture encore déposée.
    AppelOffre ao13 =
        publier(
            80,
            14,
            "Acquisition de deux chargeuses sur pneus 3 m³",
            TypeAppelOffre.DEMANDE_PRIX,
            CategorieMateriel.MATERIEL_ROULANT,
            2,
            "90000000",
            null,
            criteres(CritereEvaluation.PRIX, 60, CritereEvaluation.DELAI, 40),
            List.of(SAHEL, TERANGA));
    ao13 = offre(ao13, SAHEL, "84000000", 40, 12, "Deux Caterpillar 950 GC, godet 3,1 m³", 74);
    ao13 = offre(ao13, TERANGA, "88500000", 45, 12, "Deux Volvo L120H", 72);
    ao13 = cloturer(ao13);
    ao13 = attribuer(ao13, SAHEL, "Meilleur prix et meilleur délai.", 60, 58, 0);
    ContratAchat c13 = contrat(ao13, 57, "12", 40, "Livraison au parc matériel de Dakar");
    c13 = signer(c13, 50);
    c13 = engagement(c13, true, null, 48);
    c13 = expedier(c13, 16, "Acheminement par porte-char depuis l'entrepôt de Rufisque");
    c13 = jalons(c13, 40, 16);
    livrer(c13, 11, "BL-SE-2026-0301");
  }

  // --------------------------------------------------------------------------- appels d'offres

  /** Appel d'offres lancé pour un besoin : catégorie, quantité et spécifications du besoin. */
  private AppelOffre publier(
      int joursPublication,
      int duree,
      String objet,
      TypeAppelOffre type,
      String budget,
      int numeroBesoin,
      List<CriterePondere> criteres,
      List<FournisseurConnu> invites) {
    BesoinSeed b = besoinSeed(numeroBesoin);
    BesoinAAcheter besoin = besoins.parBesoin(besoinId(numeroBesoin)).orElseThrow();
    return publier(
        joursPublication,
        duree,
        objet,
        type,
        b.categorie(),
        b.quantite(),
        budget,
        besoin,
        criteres,
        invites);
  }

  private AppelOffre publier(
      int joursPublication,
      int duree,
      String objet,
      TypeAppelOffre type,
      CategorieMateriel categorie,
      int quantite,
      String budget,
      BesoinAAcheter besoin,
      List<CriterePondere> criteres,
      List<FournisseurConnu> invites) {
    Instant publication = ilYA(joursPublication, 8);
    NouvelAppelOffre donnees =
        new NouvelAppelOffre(
            objet,
            "Consultation lancée par la Direction des Achats pour les besoins des chantiers CSE.",
            type,
            besoin == null ? null : besoin.commeOrigine(),
            categorie,
            quantite,
            besoin == null ? null : besoin.specifications(),
            new BigDecimal(budget),
            publication.plus(duree, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS),
            new PonderationCriteres(criteres),
            invites,
            RA);
    int annee = publication.atZone(ZoneOffset.UTC).getYear();
    AppelOffre ao =
        appelsOffres.enregistrer(
            AppelOffre.publier(
                donnees, () -> references.prochaineReferenceAppelOffre(annee), publication));
    if (besoin != null) {
      besoins.enregistrer(besoin.rattacherA(ao.getId()));
    }
    evenements.appelOffre(EventTypes.AO_PUBLIE, ao);
    return ao;
  }

  private AppelOffre offre(
      AppelOffre ao,
      FournisseurConnu fournisseur,
      String montant,
      int delai,
      Integer garantie,
      String description,
      int joursDepot) {
    ao.deposerOffre(
        fournisseur, new BigDecimal(montant), delai, garantie, description, ilYA(joursDepot, 15));
    return publierAo(EventTypes.OFFRE_DEPOSEE, ao);
  }

  private AppelOffre cloturer(AppelOffre ao) {
    ao.cloturer(ao.getDateLimite());
    return publierAo(EventTypes.AO_CLOTURE, ao);
  }

  private AppelOffre noter(
      AppelOffre ao, FournisseurConnu fournisseur, Map<CritereEvaluation, Integer> notes) {
    ao.evaluerOffre(ao.offreDu(fournisseur.id()).orElseThrow().getId(), notes);
    return appelsOffres.enregistrer(ao);
  }

  private AppelOffre proposer(
      AppelOffre ao, FournisseurConnu fournisseur, String justification, int jours) {
    ao.proposerAttribution(
        ao.offreDu(fournisseur.id()).orElseThrow().getId(),
        justification,
        RA,
        ilYA(jours, 10),
        parametres.seuilDelegation());
    return publierAo(EventTypes.ATTRIBUTION_PROPOSEE, ao);
  }

  private AppelOffre attribuer(
      AppelOffre ao,
      FournisseurConnu fournisseur,
      String justification,
      int joursProposition,
      int joursRm,
      int joursDg) {
    AppelOffre propose = proposer(ao, fournisseur, justification, joursProposition);
    propose.coValiderAttribution(true, "Conforme au besoin exprimé.", RM, ilYA(joursRm, 11));
    if (propose.getAttribution().orElseThrow().approbationDgRequise()) {
      propose.approuverAttribution(true, "Approuvé en comité de direction.", DG, ilYA(joursDg, 16));
    }
    return publierAo(EventTypes.MARCHE_ATTRIBUE, propose);
  }

  private AppelOffre publierAo(String type, AppelOffre ao) {
    AppelOffre enregistre = appelsOffres.enregistrer(ao);
    evenements.appelOffre(type, enregistre);
    return enregistre;
  }

  // --------------------------------------------------------------------------------- contrats

  private ContratAchat contrat(
      AppelOffre ao, int jours, String garantie, int delai, String conditionsLivraison) {
    int annee = ilYA(jours, 9).atZone(ZoneOffset.UTC).getYear();
    ContratAchat c =
        ContratAchat.generer(
            ao, () -> references.prochainNumeroContrat(annee), parametres.contrat());
    if (conditionsLivraison != null) {
      c.modifierClauses(
          new ClausesContrat(
              garantie == null ? null : Integer.valueOf(garantie),
              "Entretien des 500 premières heures à la charge du fournisseur",
              conditionsLivraison,
              parametres.contrat().penaliteRetardPctParJourDefaut()),
          delai,
          List.of());
    }
    return publierContrat(EventTypes.CONTRAT_GENERE, c);
  }

  /** Signature en deux temps : le CSE la veille, le fournisseur le jour {@code jours}. */
  private ContratAchat signer(ContratAchat c, int jours) {
    ContratAchat envoye = envoyerEnSignature(c, jours + 1);
    envoye.signerPourLeFournisseur(representant(envoye), ilYA(jours, 10));
    ContratAchat signe = publierContrat(EventTypes.CONTRAT_SIGNE, envoye);
    evenements.contrat(EventTypes.ENGAGEMENT_DEMANDE, signe);
    return signe;
  }

  /** Signature du CSE (jalons planifiés relativement à la date de signature prévue). */
  private ContratAchat envoyerEnSignature(ContratAchat c, int jours) {
    LocalDate signature = date(jours);
    int delai = c.getDelaiLivraisonJours();
    c.modifierClauses(
        c.getClauses(),
        delai,
        List.of(
            new NouveauJalon("Validation des plans de livraison", signature.plusDays(delai / 4)),
            new NouveauJalon(
                "Expédition du matériel", signature.plusDays(delai - Math.max(2, delai / 10))),
            new NouveauJalon("Livraison sur chantier", signature.plusDays(delai))));
    c.signerPourLeCse(RA, ilYA(jours, 16));
    return publierContrat(EventTypes.CONTRAT_ENVOYE_SIGNATURE, c);
  }

  private ContratAchat refuserSignature(ContratAchat c, String motif) {
    c.refuserSignature(motif);
    return publierContrat(EventTypes.CONTRAT_SIGNATURE_REFUSEE, c);
  }

  /** Signataire côté fournisseur : le compte portail de démonstration pour Sahel Engins. */
  private static Acteur representant(ContratAchat c) {
    return SAHEL.id().equals(c.getFournisseurId())
        ? PORTAIL_SAHEL
        : new Acteur(null, c.getFournisseurNom());
  }

  private ContratAchat engagement(ContratAchat c, boolean approuve, String commentaire, int jours) {
    c.deciderEngagement(approuve, commentaire, DAF, ilYA(jours, 14));
    return publierContrat(
        approuve ? EventTypes.ENGAGEMENT_VALIDE : EventTypes.ENGAGEMENT_REJETE, c);
  }

  /** Démarrage de la livraison par le RA (secours hors portail). */
  private ContratAchat demarrer(ContratAchat c) {
    c.demarrerLivraison();
    return publierContrat(EventTypes.LIVRAISON_DEMARREE, c);
  }

  /** Expédition déclarée par le fournisseur sur le portail. */
  private ContratAchat expedier(ContratAchat c, int jours, String commentaire) {
    c.declarerExpedition(date(jours), commentaire, LocalDate.now(clock));
    return publierContrat(EventTypes.LIVRAISON_DEMARREE, c);
  }

  /** Renseigne le premier jalon, et l'expédition si {@code joursExpedition >= 0}. */
  private ContratAchat jalons(ContratAchat c, int joursPremier, int joursExpedition) {
    LocalDate aujourdhui = LocalDate.now(clock);
    c.enregistrerJalon(c.getJalons().get(0).id(), date(joursPremier), aujourdhui);
    if (joursExpedition >= 0) {
      c.enregistrerJalon(c.getJalons().get(1).id(), date(joursExpedition), aujourdhui);
    }
    return contrats.enregistrer(c);
  }

  private ContratAchat livrer(ContratAchat c, int jours, String bonLivraison) {
    LocalDate livraison = date(jours);
    c.enregistrerJalon(c.getJalons().get(2).id(), livraison, LocalDate.now(clock));
    c.confirmerLivraison(
        livraison,
        bonLivraison,
        "Matériel réceptionné conforme, documents de bord remis.",
        LocalDate.now(clock),
        parametres.contrat().plafondPenalitesPct());
    return publierContrat(EventTypes.LIVRAISON_EFFECTUEE, c);
  }

  private void cloturerContrat(ContratAchat c) {
    c.cloturer();
    publierContrat(EventTypes.CONTRAT_CLOTURE, c);
  }

  private void retard(ContratAchat c) {
    if (c.signalerRetard(LocalDate.now(clock), parametres.contrat())) {
      publierContrat(EventTypes.ALERTE_RETARD_LIVRAISON, c);
    } else {
      contrats.enregistrer(c);
    }
  }

  private ContratAchat publierContrat(String type, ContratAchat c) {
    ContratAchat enregistre = contrats.enregistrer(c);
    evenements.contrat(type, enregistre);
    return enregistre;
  }

  // --------------------------------------------------------------------------------- factures

  private Facture facture(ContratAchat c, String numero, String montantHt, int joursEmission) {
    BigDecimal ht = new BigDecimal(montantHt);
    BigDecimal deja =
        factures.parContrat(c.getId()).stream()
            .filter(x -> x.getStatut() != StatutFacture.REJETEE)
            .map(Facture::getMontantTtc)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    LocalDate emission = date(joursEmission);
    Facture f =
        factures.enregistrer(
            Facture.recevoir(
                c,
                numero,
                ht,
                ht.multiply(TVA).setScale(0, RoundingMode.HALF_UP),
                emission,
                emission.plusDays(30),
                deja));
    evenements.facture(EventTypes.FACTURE_RECUE, f);
    return f;
  }

  private Facture mandater(Facture f, String reference, int jours) {
    f.mandater(reference, date(jours));
    return publierFacture(EventTypes.FACTURE_MANDATEE, f);
  }

  private void payer(Facture f, int jours) {
    f.payer(date(jours));
    publierFacture(EventTypes.FACTURE_PAYEE, f);
  }

  private void rejeter(Facture f, String motif) {
    f.rejeter(motif);
    publierFacture(EventTypes.FACTURE_REJETEE, f);
  }

  private Facture publierFacture(String type, Facture f) {
    Facture enregistree = factures.enregistrer(f);
    evenements.facture(type, enregistree);
    return enregistree;
  }

  // ---------------------------------------------------------------------------------- outils

  private BesoinAAcheter projection(BesoinSeed b) {
    // Instant d'orientation vers l'achat dans ms-besoins : soumission à 8 h + délai de décision.
    Instant reception =
        LocalDate.now(clock)
            .minusDays(b.joursAvant())
            .atStartOfDay(ZoneOffset.UTC)
            .plusHours(8L + b.heuresDecision())
            .plusMinutes(b.numero() + 1L)
            .toInstant();
    return new BesoinAAcheter(
        besoinId(b.numero()),
        referenceBesoin(b.numero()),
        b.designation(),
        b.categorie(),
        b.quantite(),
        b.specifications(),
        b.priorite(),
        chantierId(b.chantier()),
        CHANTIERS_BESOINS.get(b.chantier()),
        reception,
        null);
  }

  /**
   * Reproduit la référence attribuée par ms-besoins : séquence par année, besoins soumis du plus
   * ancien au plus récent.
   */
  private String referenceBesoin(int numero) {
    LocalDate aujourdhui = LocalDate.now(clock);
    int joursCible = SOUMISSIONS_BESOINS.get(numero);
    int annee = aujourdhui.minusDays(joursCible).getYear();
    long rang =
        SOUMISSIONS_BESOINS.values().stream()
            .filter(j -> j >= joursCible)
            .filter(j -> aujourdhui.minusDays(j).getYear() == annee)
            .count();
    return String.format("BES-%d-%04d", annee, rang);
  }

  private static BesoinSeed besoinSeed(int numero) {
    return BESOINS_EN_ACHAT.stream().filter(b -> b.numero() == numero).findFirst().orElseThrow();
  }

  private static UUID besoinId(int numero) {
    return id(String.format("b0000000-0000-0000-0000-%012d", numero));
  }

  private static UUID chantierId(int numero) {
    return id(String.format("c0000000-0000-0000-0000-%012d", numero));
  }

  private static List<CriterePondere> criteres(Object... codesEtPoids) {
    List<CriterePondere> liste = new ArrayList<>();
    for (int i = 0; i < codesEtPoids.length; i += 2) {
      liste.add(
          new CriterePondere((CritereEvaluation) codesEtPoids[i], (Integer) codesEtPoids[i + 1]));
    }
    return liste;
  }

  private Instant ilYA(int jours, int heure) {
    return LocalDate.now(clock)
        .minusDays(jours)
        .atStartOfDay(ZoneOffset.UTC)
        .plusHours(heure)
        .toInstant();
  }

  private LocalDate date(int jours) {
    return LocalDate.now(clock).minusDays(jours);
  }

  private static FournisseurConnu fournisseur(int n, String nom, StatutFournisseur statut) {
    return new FournisseurConnu(id(String.format("f0000000-0000-0000-0000-%012d", n)), nom, statut);
  }

  private static UUID id(String valeur) {
    return UUID.fromString(valeur);
  }
}
