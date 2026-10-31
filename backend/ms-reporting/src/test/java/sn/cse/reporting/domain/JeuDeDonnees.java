package sn.cse.reporting.domain;

import static java.math.BigDecimal.valueOf;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.DonneesReporting;
import sn.cse.reporting.domain.model.EtatMateriel;
import sn.cse.reporting.domain.model.GraviteLitige;
import sn.cse.reporting.domain.model.GravitePanne;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.Priorite;
import sn.cse.reporting.domain.model.StatutAffectation;
import sn.cse.reporting.domain.model.StatutAppelOffre;
import sn.cse.reporting.domain.model.StatutBesoin;
import sn.cse.reporting.domain.model.StatutChantier;
import sn.cse.reporting.domain.model.StatutContrat;
import sn.cse.reporting.domain.model.StatutFacture;
import sn.cse.reporting.domain.model.StatutFournisseur;
import sn.cse.reporting.domain.model.StatutLitige;
import sn.cse.reporting.domain.model.StatutMaintenance;
import sn.cse.reporting.domain.model.TypeMaintenance;
import sn.cse.reporting.domain.model.vue.AffectationVue;
import sn.cse.reporting.domain.model.vue.AppelOffreVue;
import sn.cse.reporting.domain.model.vue.BesoinVue;
import sn.cse.reporting.domain.model.vue.ChantierVue;
import sn.cse.reporting.domain.model.vue.ContratVue;
import sn.cse.reporting.domain.model.vue.FactureVue;
import sn.cse.reporting.domain.model.vue.FournisseurVue;
import sn.cse.reporting.domain.model.vue.LitigeVue;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.MaterielVue;
import sn.cse.reporting.domain.model.vue.PanneVue;

/**
 * Jeu de vues construit à la main, dont les indicateurs attendus sont calculés dans les
 * commentaires des tests. Aujourd'hui = 2026-10-30 ; période = octobre (1er → 30, 30 jours).
 */
public final class JeuDeDonnees {

  public static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 30);
  public static final Instant MAINTENANT = Instant.parse("2026-10-30T12:00:00Z");
  public static final Periode OCTOBRE = new Periode(LocalDate.of(2026, 10, 1), AUJOURDHUI);

  public static final UUID C1 = id("c1");
  public static final UUID C2 = id("c2");
  public static final UUID C3 = id("c3");
  public static final UUID M1 = id("m1");
  public static final UUID M2 = id("m2");
  public static final UUID M3 = id("m3");
  public static final UUID M4 = id("m4");
  public static final UUID F1 = id("f1");
  public static final UUID F2 = id("f2");
  public static final UUID F3 = id("f3");
  public static final UUID AO1 = id("ao1");
  public static final UUID AO2 = id("ao2");

  private JeuDeDonnees() {}

  public static UUID id(String graine) {
    return UUID.nameUUIDFromBytes(graine.getBytes());
  }

  static LocalDate j(int mois, int jour) {
    return LocalDate.of(2026, mois, jour);
  }

  static Instant t(String iso) {
    return Instant.parse(iso);
  }

  public static DonneesReporting donnees() {
    Instant maj = t("2026-10-29T00:00:00Z");
    List<ChantierVue> chantiers =
        List.of(
            new ChantierVue(
                C1,
                "CH-1",
                "Autoroute",
                "Dakar",
                StatutChantier.EN_COURS,
                valueOf(10_000_000),
                valueOf(36_500_000),
                null,
                j(1, 1),
                j(12, 31),
                true,
                maj),
            new ChantierVue(
                C2,
                "CH-2",
                "Pont",
                "Saint-Louis",
                StatutChantier.EN_COURS,
                valueOf(5_000_000),
                valueOf(0),
                null,
                j(6, 1),
                null,
                true,
                maj),
            new ChantierVue(
                C3,
                "CH-3",
                "Station",
                "Mbour",
                StatutChantier.TERMINE,
                valueOf(9_000_000),
                valueOf(9_000_000),
                null,
                j(1, 1),
                j(8, 31),
                true,
                maj));
    List<MaterielVue> materiels =
        List.of(
            new MaterielVue(
                M1,
                "M1",
                "Pelle",
                CategorieMateriel.MATERIEL_ROULANT,
                EtatMateriel.AFFECTE,
                C1,
                valueOf(100_000),
                null,
                null,
                maj),
            new MaterielVue(
                M2,
                "M2",
                "Camion",
                CategorieMateriel.MATERIEL_ROULANT,
                EtatMateriel.DISPONIBLE,
                null,
                valueOf(50_000),
                null,
                null,
                maj),
            new MaterielVue(
                M3,
                "M3",
                "Perceuse",
                CategorieMateriel.OUTILLAGE,
                EtatMateriel.EN_MAINTENANCE,
                C1,
                valueOf(10_000),
                null,
                maj,
                maj),
            new MaterielVue(
                M4,
                "M4",
                "Scie",
                CategorieMateriel.OUTILLAGE,
                EtatMateriel.REFORME,
                null,
                valueOf(5_000),
                null,
                null,
                maj));
    List<AffectationVue> affectations =
        List.of(
            // M1 → C1 du 11/10, en cours : 20 jours, 2 000 000 FCFA.
            new AffectationVue(
                id("a1"),
                M1,
                CategorieMateriel.MATERIEL_ROULANT,
                C1,
                valueOf(100_000),
                null,
                j(10, 11),
                j(12, 31),
                null,
                StatutAffectation.EN_COURS,
                maj),
            // M2 → C2 du 20/09 au 05/10 : 5 jours dans la période, 250 000 FCFA.
            new AffectationVue(
                id("a2"),
                M2,
                CategorieMateriel.MATERIEL_ROULANT,
                C2,
                valueOf(50_000),
                null,
                j(9, 20),
                j(10, 5),
                j(10, 5),
                StatutAffectation.TERMINEE,
                maj),
            // M3 → C1 depuis le 01/10 : 30 jours facturés, 300 000 FCFA (immobilisé à partir du
            // 21).
            new AffectationVue(
                id("a3"),
                M3,
                CategorieMateriel.OUTILLAGE,
                C1,
                valueOf(10_000),
                null,
                j(10, 1),
                null,
                null,
                StatutAffectation.EN_COURS,
                maj));
    List<MaintenanceVue> maintenances =
        List.of(
            new MaintenanceVue(
                id("mt1"),
                "OT-1",
                M3,
                C1,
                TypeMaintenance.CORRECTIVE,
                StatutMaintenance.EN_COURS,
                null,
                j(10, 20),
                j(10, 21),
                null,
                null,
                maj),
            new MaintenanceVue(
                id("mt2"),
                "OT-2",
                M2,
                null,
                TypeMaintenance.PREVENTIVE,
                StatutMaintenance.CLOTUREE,
                valueOf(300_000),
                j(10, 6),
                j(10, 6),
                j(10, 7),
                t("2026-10-07T18:00:00Z"),
                maj));
    List<PanneVue> pannes =
        List.of(
            new PanneVue(id("p1"), M1, C1, GravitePanne.MINEURE, t("2026-10-15T10:00:00Z"), null),
            new PanneVue(
                id("p2"),
                M2,
                null,
                GravitePanne.MAJEURE,
                t("2026-10-05T10:00:00Z"),
                t("2026-10-07T18:00:00Z")));
    List<BesoinVue> besoins =
        List.of(
            new BesoinVue(
                id("b1"),
                "BES-1",
                C1,
                "Autoroute",
                CategorieMateriel.MATERIEL_ROULANT,
                1,
                Priorite.NORMAL,
                StatutBesoin.EN_ATTENTE,
                j(11, 15),
                t("2026-10-03T10:00:00Z"),
                null,
                t("2026-10-03T10:00:00Z"),
                maj),
            new BesoinVue(
                id("b2"),
                "BES-2",
                C2,
                "Pont",
                CategorieMateriel.OUTILLAGE,
                2,
                Priorite.URGENT,
                StatutBesoin.AFFECTE_DIRECT,
                j(11, 1),
                t("2026-10-05T08:00:00Z"),
                t("2026-10-05T20:00:00Z"),
                t("2026-10-05T08:00:00Z"),
                maj),
            new BesoinVue(
                id("b3"),
                "BES-3",
                C1,
                "Autoroute",
                CategorieMateriel.OUTILLAGE,
                1,
                Priorite.CRITIQUE,
                StatutBesoin.REJETE,
                j(11, 2),
                t("2026-10-10T00:00:00Z"),
                t("2026-10-11T00:00:00Z"),
                t("2026-10-10T00:00:00Z"),
                maj),
            new BesoinVue(
                id("b4"),
                "BES-4",
                C2,
                "Pont",
                CategorieMateriel.CONSOMMABLE,
                10,
                Priorite.NORMAL,
                StatutBesoin.EN_ACHAT,
                j(10, 1),
                t("2026-09-01T09:00:00Z"),
                t("2026-09-02T09:00:00Z"),
                t("2026-09-01T09:00:00Z"),
                maj));
    List<AppelOffreVue> aos =
        List.of(
            new AppelOffreVue(
                AO1,
                "AO-1",
                C1,
                null,
                CategorieMateriel.MATERIEL_ROULANT,
                StatutAppelOffre.ATTRIBUE,
                t("2026-09-10T08:00:00Z"),
                t("2026-09-25T17:00:00Z"),
                valueOf(3_000_000),
                F1,
                maj),
            new AppelOffreVue(
                AO2,
                "AO-2",
                C2,
                null,
                CategorieMateriel.OUTILLAGE,
                StatutAppelOffre.ATTRIBUE,
                t("2026-08-01T08:00:00Z"),
                t("2026-08-20T17:00:00Z"),
                valueOf(1_000_000),
                F2,
                maj),
            new AppelOffreVue(
                id("ao3"),
                "AO-3",
                C1,
                null,
                CategorieMateriel.OUTILLAGE,
                StatutAppelOffre.PUBLIE,
                t("2026-10-05T08:00:00Z"),
                t("2026-11-05T17:00:00Z"),
                null,
                null,
                maj),
            new AppelOffreVue(
                id("ao4"),
                "AO-4",
                C2,
                null,
                CategorieMateriel.OUTILLAGE,
                StatutAppelOffre.CLOTURE,
                t("2026-10-07T08:00:00Z"),
                t("2026-10-20T17:00:00Z"),
                null,
                null,
                maj));
    List<ContratVue> contrats =
        List.of(
            new ContratVue(
                id("k1"),
                "CTR-1",
                AO1,
                C1,
                F1,
                "Sahel Engins",
                valueOf(3_000_000),
                StatutContrat.LIVRE,
                j(10, 2),
                j(10, 2),
                j(10, 20),
                j(10, 18),
                maj),
            new ContratVue(
                id("k2"),
                "CTR-2",
                AO2,
                C2,
                F2,
                "Teranga",
                valueOf(1_000_000),
                StatutContrat.LIVRE,
                j(9, 15),
                j(9, 15),
                j(10, 10),
                j(10, 15),
                maj),
            new ContratVue(
                id("k3"),
                "CTR-3",
                null,
                C1,
                F1,
                "Sahel Engins",
                valueOf(2_000_000),
                StatutContrat.EN_LIVRAISON,
                j(10, 25),
                j(10, 25),
                j(10, 28),
                null,
                maj));
    List<FactureVue> factures =
        List.of(
            new FactureVue(
                id("fa1"),
                "FA-1",
                id("k1"),
                F1,
                valueOf(1_500_000),
                StatutFacture.MANDATEE,
                j(11, 20),
                j(10, 19),
                j(10, 20),
                null,
                maj),
            new FactureVue(
                id("fa2"),
                "FA-2",
                id("k2"),
                F2,
                valueOf(800_000),
                StatutFacture.PAYEE,
                j(10, 30),
                j(9, 20),
                j(9, 30),
                j(10, 10),
                maj),
            new FactureVue(
                id("fa3"),
                "FA-3",
                id("k1"),
                F1,
                valueOf(500_000),
                StatutFacture.REJETEE,
                j(11, 20),
                j(10, 19),
                null,
                null,
                maj));
    List<FournisseurVue> fournisseurs =
        List.of(
            new FournisseurVue(F1, "Sahel Engins", StatutFournisseur.ACTIF, valueOf(80), true, maj),
            new FournisseurVue(F2, "Teranga", StatutFournisseur.ACTIF, valueOf(60), true, maj),
            new FournisseurVue(
                F3, "Ndiambour", StatutFournisseur.SUSPENDU, valueOf(90), true, maj));
    List<LitigeVue> litiges =
        List.of(
            new LitigeVue(
                id("l1"),
                "LIT-1",
                F2,
                id("k2"),
                GraviteLitige.MOYENNE,
                StatutLitige.OUVERT,
                maj,
                maj),
            new LitigeVue(
                id("l2"), "LIT-2", F1, null, GraviteLitige.FAIBLE, StatutLitige.RESOLU, maj, maj));
    return new DonneesReporting(
        chantiers,
        materiels,
        affectations,
        maintenances,
        pannes,
        besoins,
        aos,
        contrats,
        factures,
        fournisseurs,
        litiges);
  }
}
