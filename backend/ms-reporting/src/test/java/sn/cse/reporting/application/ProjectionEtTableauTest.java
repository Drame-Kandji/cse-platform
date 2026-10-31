package sn.cse.reporting.application;

import static java.math.BigDecimal.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;
import sn.cse.events.EventTypes;
import sn.cse.events.payload.AffectationPayload;
import sn.cse.events.payload.AppelOffrePayload;
import sn.cse.events.payload.BesoinPayload;
import sn.cse.events.payload.ChantierPayload;
import sn.cse.events.payload.ContratPayload;
import sn.cse.events.payload.FacturePayload;
import sn.cse.events.payload.FournisseurPayload;
import sn.cse.events.payload.LitigePayload;
import sn.cse.events.payload.MaintenancePayload;
import sn.cse.events.payload.MaterielPayload;
import sn.cse.events.payload.PannePayload;
import sn.cse.reporting.application.command.DemandeSerie;
import sn.cse.reporting.application.command.DemandeTableau;
import sn.cse.reporting.application.command.EvenementRecu;
import sn.cse.reporting.application.command.FichierExporte;
import sn.cse.reporting.application.command.LecteurPayload;
import sn.cse.reporting.application.port.out.ExportateurTableau;
import sn.cse.reporting.application.service.ConsulterSerieService;
import sn.cse.reporting.application.service.ConsulterTableauService;
import sn.cse.reporting.application.service.ExporterTableauService;
import sn.cse.reporting.application.service.PhotographierParcService;
import sn.cse.reporting.application.service.ProjectionAchatsService;
import sn.cse.reporting.application.service.ProjectionBesoinsService;
import sn.cse.reporting.application.service.ProjectionFournisseursService;
import sn.cse.reporting.application.service.ProjectionSuiviService;
import sn.cse.reporting.domain.model.EtatMateriel;
import sn.cse.reporting.domain.model.FormatExport;
import sn.cse.reporting.domain.model.ParametresReporting;
import sn.cse.reporting.domain.model.Point;
import sn.cse.reporting.domain.model.Tableau;

/**
 * Cas d'utilisation de bout en bout sur une persistance en mémoire : séquences d'événements
 * construites à la main, idempotence (un événement rejoué n'est compté qu'une fois) et ordre des
 * événements non garanti entre agrégats.
 */
class ProjectionEtTableauTest {

  private static final Instant MAINTENANT = Instant.parse("2026-10-30T12:00:00Z");
  private static final LocalDate AUJ = LocalDate.of(2026, 10, 30);
  private static final UUID C = UUID.randomUUID();
  private static final UUID M = UUID.randomUUID();
  private static final UUID M2 = UUID.randomUUID();
  private static final UUID F = UUID.randomUUID();
  private static final UUID AO = UUID.randomUUID();
  private static final UUID K = UUID.randomUUID();
  private static final ParametresReporting PARAMS = new ParametresReporting(90, 1830, 400, 5, 365);

  private final Clock clock = Clock.fixed(MAINTENANT, ZoneOffset.UTC);
  private final ReportingEnMemoire vues = new ReportingEnMemoire();
  private final PhotographierParcService photo = new PhotographierParcService(vues);
  private final ProjectionSuiviService suivi = new ProjectionSuiviService(vues, photo, clock);
  private final ProjectionBesoinsService besoins = new ProjectionBesoinsService(vues);
  private final ProjectionFournisseursService fournisseurs =
      new ProjectionFournisseursService(vues);
  private final ProjectionAchatsService achats = new ProjectionAchatsService(vues);
  private final ConsulterTableauService tableaux = new ConsulterTableauService(vues, PARAMS, clock);

  private static EvenementRecu evt(String type, Object payload, Instant quand) {
    return evt(UUID.randomUUID(), type, payload, quand);
  }

  private static EvenementRecu evt(UUID id, String type, Object payload, Instant quand) {
    return new EvenementRecu(
        id,
        type,
        quand,
        new LecteurPayload() {
          @Override
          public <T> T lire(Class<T> classe) {
            return classe.cast(payload);
          }
        });
  }

  private static Instant t(String iso) {
    return Instant.parse(iso);
  }

  private static CurrentUser utilisateur(String role, Set<UUID> chantiers) {
    return new CurrentUser(UUID.randomUUID(), "U", "u@cse.sn", Set.of(role), chantiers, null);
  }

  private Tableau tableau(String role) {
    return tableaux.tableau(
        new DemandeTableau(
            utilisateur(Roles.DIRECTION_GENERALE, Set.of()),
            role,
            LocalDate.of(2026, 10, 1),
            AUJ,
            null));
  }

  private static AffectationPayload affectation(UUID id, String statut) {
    return new AffectationPayload(
        id,
        M,
        "M-1",
        "Pelle",
        "MATERIEL_ROULANT",
        valueOf(100_000),
        C,
        "Autoroute",
        null,
        null,
        LocalDate.of(2026, 10, 11),
        LocalDate.of(2026, 12, 31),
        null,
        statut,
        null,
        null);
  }

  @Test
  void should_compter_une_seule_fois_et_resister_au_desordre_when_evenements_suivi() {
    suivi.projeter(
        evt(
            EventTypes.CHANTIER_CREE,
            new ChantierPayload(
                C,
                "CH-1",
                "Autoroute",
                "Dakar",
                "EN_COURS",
                valueOf(10_000_000),
                valueOf(36_500_000),
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)),
            t("2026-01-01T08:00:00Z")));
    UUID a = UUID.randomUUID();
    EvenementRecu affecte =
        evt(
            EventTypes.MATERIEL_AFFECTE,
            affectation(a, "EN_ATTENTE_RECEPTION"),
            t("2026-10-11T09:00:00Z"));
    assertThat(suivi.projeter(affecte)).isTrue();
    // Rejeu (livraison « au moins une fois ») : sans effet.
    assertThat(suivi.projeter(affecte)).isTrue();
    // Enregistrement du matériel plus ancien, reçu après l'affectation (autre agrégat).
    suivi.projeter(
        evt(
            EventTypes.MATERIEL_ENREGISTRE,
            new MaterielPayload(
                M,
                "M-1",
                "Pelle CAT 320",
                "MATERIEL_ROULANT",
                "DISPONIBLE",
                null,
                null,
                valueOf(100_000),
                valueOf(90_000_000),
                0,
                null,
                null,
                null),
            t("2026-10-01T09:00:00Z")));

    assertThat(vues.affectations).hasSize(1);
    assertThat(vues.materiels.get(M).etat()).isEqualTo(EtatMateriel.AFFECTE);
    assertThat(vues.materiels.get(M).chantierId()).isEqualTo(C);
    assertThat(vues.materiels.get(M).designation()).isEqualTo("Pelle");

    Tableau rm = tableau("RESPONSABLE_MATERIEL");
    assertThat(rm.parc().total()).isEqualTo(1);
    assertThat(rm.parc().affectes()).isEqualTo(1);
    // 11/10 → 30/10 : 20 jours × 100 000, comptés une seule fois malgré le rejeu.
    assertThat(rm.chantiers().get(0).coutExploitation()).isEqualByComparingTo("2000000");
    assertThat(rm.chantiers().get(0).chantierNom()).isEqualTo("Autoroute");
    assertThat(vues.instantanes.get(AUJ))
        .singleElement()
        .satisfies(i -> assertThat(i.affectes()).isEqualTo(1));

    suivi.projeter(
        evt(
            EventTypes.AFFECTATION_TERMINEE,
            affectation(a, "TERMINEE"),
            t("2026-10-20T09:00:00Z")));
    assertThat(vues.materiels.get(M).etat()).isEqualTo(EtatMateriel.DISPONIBLE);
    assertThat(vues.materiels.get(M).chantierId()).isNull();
    assertThat(suivi.projeter(evt("INCONNU", null, MAINTENANT))).isFalse();
  }

  @Test
  void should_resoudre_pannes_et_alertes_when_maintenance_cloturee() {
    suivi.projeter(
        evt(
            EventTypes.MATERIEL_ENREGISTRE,
            new MaterielPayload(
                M2,
                "M-2",
                "Camion",
                "MATERIEL_ROULANT",
                "DISPONIBLE",
                null,
                null,
                valueOf(50_000),
                null,
                0,
                null,
                null,
                null),
            t("2026-10-01T09:00:00Z")));
    suivi.projeter(
        evt(
            EventTypes.ALERTE_MAINTENANCE,
            new MaterielPayload(
                M2,
                "M-2",
                "Camion",
                "MATERIEL_ROULANT",
                "DISPONIBLE",
                null,
                null,
                valueOf(50_000),
                null,
                520,
                null,
                null,
                null),
            t("2026-10-02T09:00:00Z")));
    UUID p1 = UUID.randomUUID();
    suivi.projeter(
        evt(
            EventTypes.PANNE_SIGNALEE,
            new PannePayload(
                p1, M2, "Camion", C, "Autoroute", "MAJEURE", "Fuite", UUID.randomUUID(), "Chef"),
            t("2026-10-03T09:00:00Z")));
    assertThat(tableau("DG").parc().alertesMaintenance()).isEqualTo(1);
    assertThat(tableau("DG").parc().pannesOuvertes()).isEqualTo(1);

    UUID mt = UUID.randomUUID();
    suivi.projeter(
        evt(
            EventTypes.MAINTENANCE_PLANIFIEE,
            new MaintenancePayload(
                mt,
                "OT-1",
                M2,
                "Camion",
                null,
                "CORRECTIVE",
                "PLANIFIEE",
                null,
                LocalDate.of(2026, 10, 5),
                null,
                null),
            t("2026-10-04T09:00:00Z")));
    suivi.projeter(
        evt(
            EventTypes.MAINTENANCE_DEMARREE,
            new MaintenancePayload(
                mt,
                "OT-1",
                M2,
                "Camion",
                null,
                "CORRECTIVE",
                "EN_COURS",
                null,
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 6),
                null),
            t("2026-10-06T09:00:00Z")));
    assertThat(vues.materiels.get(M2).etat()).isEqualTo(EtatMateriel.EN_MAINTENANCE);
    assertThat(tableau("DG").parc().maintenancesEnCours()).isEqualTo(1);
    assertThat(tableau("DG").parc().alertesMaintenance()).isZero();

    suivi.projeter(
        evt(
            EventTypes.MAINTENANCE_CLOTUREE,
            new MaintenancePayload(
                mt,
                "OT-1",
                M2,
                "Camion",
                null,
                "CORRECTIVE",
                "CLOTUREE",
                valueOf(300_000),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 6),
                LocalDate.of(2026, 10, 7)),
            t("2026-10-07T17:00:00Z")));
    // Panne signalée avant la clôture mais reçue après : résolue à l'arrivée.
    UUID p2 = UUID.randomUUID();
    suivi.projeter(
        evt(
            EventTypes.PANNE_SIGNALEE,
            new PannePayload(
                p2, M2, "Camion", null, null, "MINEURE", "Bruit", UUID.randomUUID(), "RM"),
            t("2026-10-07T10:00:00Z")));
    suivi.projeter(
        evt(
            EventTypes.PANNE_SIGNALEE,
            new PannePayload(
                p2, M2, "Camion", null, null, "MINEURE", "Bruit", UUID.randomUUID(), "RM"),
            t("2026-10-07T10:00:00Z")));

    Tableau dg = tableau("DG");
    assertThat(vues.materiels.get(M2).etat()).isEqualTo(EtatMateriel.DISPONIBLE);
    assertThat(dg.parc().pannesOuvertes()).isZero();
    assertThat(dg.parc().maintenancesEnCours()).isZero();
    assertThat(dg.finances().coutMaintenance()).isEqualByComparingTo("300000");
    assertThat(vues.pannes).hasSize(2);
  }

  @Test
  void should_mesurer_delai_de_traitement_when_besoin_soumis_puis_decide() {
    UUID b = UUID.randomUUID();
    Instant soumission = t("2026-10-05T08:00:00Z");
    EvenementRecu soumis = evt(EventTypes.BESOIN_SOUMIS, besoin(b, "EN_ATTENTE"), soumission);
    besoins.projeter(soumis);
    besoins.projeter(
        evt(EventTypes.BESOIN_VALIDE, besoin(b, "VALIDE"), soumission.plusSeconds(6 * 3600)));
    besoins.projeter(
        evt(EventTypes.BESOIN_EN_ACHAT, besoin(b, "EN_ACHAT"), soumission.plusSeconds(7 * 3600)));
    besoins.projeter(soumis);

    Tableau.Besoins s = tableau("DG").besoins();
    assertThat(s.total()).isEqualTo(1);
    assertThat(s.enAchat()).isEqualTo(1);
    assertThat(s.delaiMoyenTraitementHeures()).isEqualByComparingTo("6.0");
    assertThat(vues.chantiers.get(C).nom()).isEqualTo("Autoroute");
    assertThat(besoins.projeter(evt(EventTypes.AO_PUBLIE, null, MAINTENANT))).isFalse();
  }

  private static BesoinPayload besoin(UUID id, String statut) {
    return new BesoinPayload(
        id,
        "BES-2026-0001",
        C,
        "Autoroute",
        UUID.randomUUID(),
        "Chef",
        "Pelle",
        "MATERIEL_ROULANT",
        1,
        null,
        "URGENT",
        statut,
        LocalDate.of(2026, 11, 1),
        30,
        null,
        List.of());
  }

  private static ContratPayload contrat(String statut, LocalDate reelle) {
    return new ContratPayload(
        K,
        "CTR-2026-001",
        AO,
        null,
        C,
        "Autoroute",
        F,
        "Sahel Engins",
        "Pelle",
        "MATERIEL_ROULANT",
        1,
        valueOf(3_000_000),
        statut,
        LocalDate.of(2026, 10, 12),
        LocalDate.of(2026, 10, 25),
        reelle,
        null);
  }

  private static FacturePayload facture(UUID id, String statut) {
    // Ancien format : sans dates métier (repli sur le jour de l'événement).
    return new FacturePayload(
        id, "FA-1", K, F, valueOf(1_200_000), statut, null, LocalDate.of(2026, 11, 30), null, null);
  }

  @Test
  void should_alimenter_achats_finances_et_fournisseurs_when_cycle_complet() {
    UUID litige = UUID.randomUUID();
    fournisseurs.projeter(
        evt(
            EventTypes.LITIGE_OUVERT,
            new LitigePayload(litige, "LIT-1", F, "Sahel Engins", K, "Retard", "MOYENNE", "OUVERT"),
            t("2026-10-02T08:00:00Z")));
    assertThat(vues.fournisseurs.get(F).complet()).isFalse();
    fournisseurs.projeter(
        evt(
            EventTypes.FOURNISSEUR_VALIDE,
            new FournisseurPayload(
                F,
                "Sahel Engins SA",
                "contact@sahel.sn",
                List.of("MATERIEL_ROULANT"),
                "ACTIF",
                valueOf(80),
                null),
            t("2026-01-01T08:00:00Z")));
    achats.projeter(
        evt(
            EventTypes.AO_PUBLIE,
            new AppelOffrePayload(
                AO,
                "AO-2026-001",
                "Pelle",
                "APPEL_OFFRES",
                null,
                C,
                "MATERIEL_ROULANT",
                1,
                t("2026-10-10T17:00:00Z"),
                "PUBLIE",
                0,
                List.of(F),
                null,
                null,
                null,
                false),
            t("2026-10-01T08:00:00Z")));
    achats.projeter(
        evt(EventTypes.CONTRAT_SIGNE, contrat("SIGNE", null), t("2026-10-12T08:00:00Z")));
    assertThat(tableau("DAF").finances().montantEngage()).isEqualByComparingTo("0");
    EvenementRecu engagement =
        evt(EventTypes.ENGAGEMENT_VALIDE, contrat("SIGNE", null), t("2026-10-14T08:00:00Z"));
    achats.projeter(engagement);
    achats.projeter(engagement);
    achats.projeter(
        evt(
            EventTypes.LIVRAISON_EFFECTUEE,
            contrat("LIVRE", LocalDate.of(2026, 10, 21)),
            t("2026-10-21T15:00:00Z")));
    UUID fa = UUID.randomUUID();
    achats.projeter(evt(EventTypes.FACTURE_RECUE, facture(fa, "RECUE"), t("2026-10-21T16:00:00Z")));
    achats.projeter(
        evt(EventTypes.FACTURE_MANDATEE, facture(fa, "MANDATEE"), t("2026-10-22T09:00:00Z")));
    achats.projeter(evt(EventTypes.FACTURE_PAYEE, facture(fa, "PAYEE"), t("2026-10-28T09:00:00Z")));

    Tableau dg = tableau("DG");
    assertThat(dg.finances().montantEngage()).isEqualByComparingTo("3000000");
    assertThat(dg.finances().montantMandate()).isEqualByComparingTo("1200000");
    assertThat(dg.finances().montantPaye()).isEqualByComparingTo("1200000");

    // Facture historique publiée aujourd'hui mais payée en juin : hors des 90 derniers jours.
    UUID ancienne = UUID.randomUUID();
    achats.projeter(
        evt(
            EventTypes.FACTURE_PAYEE,
            new FacturePayload(
                ancienne,
                "FA-0",
                K,
                F,
                valueOf(5_000_000),
                "PAYEE",
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 6, 1)),
            t("2026-10-29T09:00:00Z")));
    assertThat(vues.factures.get(ancienne).datePaiement()).isEqualTo(LocalDate.of(2026, 6, 1));
    Tableau apres = tableau("DG");
    assertThat(apres.finances().montantPaye()).isEqualByComparingTo("1200000");
    assertThat(apres.finances().montantMandate()).isEqualByComparingTo("1200000");
    // 01/10 → 21/10
    assertThat(dg.achats().delaiMoyenApprovisionnementJours()).isEqualByComparingTo("20.0");
    assertThat(dg.achats().tauxLivraisonALHeure()).isEqualByComparingTo("100.0");
    assertThat(dg.achats().contratsEnCours()).isZero();
    assertThat(dg.fournisseurs().top())
        .singleElement()
        .satisfies(
            f -> {
              assertThat(f.raisonSociale()).isEqualTo("Sahel Engins SA");
              assertThat(f.nombreLitiges()).isEqualTo(1);
            });
    assertThat(dg.fournisseurs().litigesOuverts()).isEqualTo(1);
    fournisseurs.projeter(
        evt(
            EventTypes.LITIGE_RESOLU,
            new LitigePayload(litige, "LIT-1", F, "Sahel Engins", K, "Retard", "MOYENNE", "RESOLU"),
            t("2026-10-25T08:00:00Z")));
    assertThat(tableau("DG").fournisseurs().litigesOuverts()).isZero();
    assertThat(
            fournisseurs.projeter(
                evt(
                    "FOURNISSEUR_MODIFIE",
                    new FournisseurPayload(
                        F,
                        "Sahel Engins Groupe",
                        "c@sahel.sn",
                        List.of(),
                        "ACTIF",
                        valueOf(85),
                        null),
                    t("2026-10-26T08:00:00Z"))))
        .isTrue();
    assertThat(vues.fournisseurs.get(F).raisonSociale()).isEqualTo("Sahel Engins Groupe");
    UUID classe = UUID.randomUUID();
    fournisseurs.projeter(
        evt(
            EventTypes.LITIGE_RESOLU,
            new LitigePayload(classe, "LIT-2", F, "Sahel", null, "Doublon", "FAIBLE", "CLASSE"),
            t("2026-10-27T08:00:00Z")));
    assertThat(tableau("DG").fournisseurs().litigesOuverts()).isZero();
    assertThat(fournisseurs.projeter(evt("TYPE_INCONNU", null, MAINTENANT))).isFalse();
    assertThat(achats.projeter(evt(EventTypes.BESOIN_SOUMIS, null, MAINTENANT))).isFalse();
    assertThat(fournisseurs.projeter(evt(EventTypes.AO_PUBLIE, null, MAINTENANT))).isFalse();
  }

  @Test
  void should_calculer_serie_et_refuser_granularite_inconnue() {
    ConsulterSerieService service = new ConsulterSerieService(vues, PARAMS, clock);
    suivi.projeter(
        evt(
            EventTypes.MATERIEL_ENREGISTRE,
            new MaterielPayload(
                M2,
                "M-2",
                "Camion",
                "MATERIEL_ROULANT",
                "DISPONIBLE",
                null,
                null,
                valueOf(50_000),
                null,
                0,
                null,
                null,
                null),
            t("2026-10-01T09:00:00Z")));
    CurrentUser rm = utilisateur(Roles.RESPONSABLE_MATERIEL, Set.of());
    List<Point> dispo =
        service.serie(
            new DemandeSerie(rm, "TAUX_DISPONIBILITE", null, LocalDate.of(2026, 10, 1), AUJ, null));
    assertThat(dispo)
        .singleElement()
        .satisfies(
            p -> {
              assertThat(p.periode()).isEqualTo("2026-10");
              assertThat(p.valeur()).isEqualByComparingTo("100.0");
            });
    assertThat(service.serie(new DemandeSerie(rm, "BESOINS_SOUMIS", "jour", null, null, null)))
        .hasSize(90);
    assertThatThrownBy(
            () -> service.serie(new DemandeSerie(rm, "BESOINS_SOUMIS", "ANNEE", null, null, null)))
        .isInstanceOf(InvalidRequestException.class)
        .extracting("code")
        .isEqualTo("GRANULARITE_INCONNUE");
  }

  @Test
  void should_nommer_fichier_exporte_selon_profil_et_date() {
    ExportateurTableau pdf =
        new ExportateurTableau() {
          @Override
          public FormatExport format() {
            return FormatExport.PDF;
          }

          @Override
          public byte[] exporter(Tableau tableau) {
            return tableau.profil().name().getBytes();
          }
        };
    ExporterTableauService service = new ExporterTableauService(tableaux, List.of(pdf));
    DemandeTableau dg =
        new DemandeTableau(utilisateur(Roles.DIRECTION_GENERALE, Set.of()), null, null, null, null);
    FichierExporte f = service.exporter(dg, "pdf");
    assertThat(f.nom()).isEqualTo("tableau-de-bord-DG-2026-10-30.pdf");
    assertThat(f.typeMime()).isEqualTo("application/pdf");
    assertThat(new String(f.contenu())).isEqualTo("DG");
    assertThat(f).isEqualTo(new FichierExporte(f.nom(), f.typeMime(), "DG".getBytes()));
    assertThat(f.hashCode())
        .isEqualTo(new FichierExporte(f.nom(), f.typeMime(), "DG".getBytes()).hashCode());
    assertThat(f.toString()).contains("2 octets");
    assertThatThrownBy(() -> service.exporter(dg, "csv"))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> service.exporter(dg, "xlsx"))
        .isInstanceOf(IllegalStateException.class);
  }
}
