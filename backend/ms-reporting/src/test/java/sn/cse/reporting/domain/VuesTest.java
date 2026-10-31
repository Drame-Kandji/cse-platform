package sn.cse.reporting.domain;

import static java.math.BigDecimal.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.EtatMateriel;
import sn.cse.reporting.domain.model.GraviteLitige;
import sn.cse.reporting.domain.model.GravitePanne;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.StatutAffectation;
import sn.cse.reporting.domain.model.StatutAppelOffre;
import sn.cse.reporting.domain.model.StatutBesoin;
import sn.cse.reporting.domain.model.StatutChantier;
import sn.cse.reporting.domain.model.StatutContrat;
import sn.cse.reporting.domain.model.StatutFacture;
import sn.cse.reporting.domain.model.StatutFournisseur;
import sn.cse.reporting.domain.model.StatutLitige;
import sn.cse.reporting.domain.model.StatutMaintenance;
import sn.cse.reporting.domain.model.vue.AffectationVue;
import sn.cse.reporting.domain.model.vue.AppelOffreVue;
import sn.cse.reporting.domain.model.vue.BesoinVue;
import sn.cse.reporting.domain.model.vue.ChantierVue;
import sn.cse.reporting.domain.model.vue.ContratVue;
import sn.cse.reporting.domain.model.vue.FactureVue;
import sn.cse.reporting.domain.model.vue.FactureVue.DatesMetier;
import sn.cse.reporting.domain.model.vue.FournisseurVue;
import sn.cse.reporting.domain.model.vue.Horodatage;
import sn.cse.reporting.domain.model.vue.LitigeVue;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.MaterielVue;
import sn.cse.reporting.domain.model.vue.PanneVue;
import sn.cse.reporting.domain.model.vue.ParcInstantane;

/** Règles de fusion défensive des vues (ordre des événements non garanti entre agrégats). */
class VuesTest {

  private static final UUID ID = UUID.randomUUID();
  private static final Instant T1 = Instant.parse("2026-10-01T10:00:00Z");
  private static final Instant T2 = Instant.parse("2026-10-02T10:00:00Z");
  private static final LocalDate AUJ = LocalDate.of(2026, 10, 30);

  private static ChantierVue chantier(String nom, Instant quand) {
    return new ChantierVue(
        ID,
        "CH",
        nom,
        "Dakar",
        StatutChantier.EN_COURS,
        valueOf(1),
        null,
        null,
        LocalDate.of(2026, 1, 1),
        null,
        true,
        quand);
  }

  @Test
  void should_completer_reference_de_chantier_puis_remplacer_par_instantane() {
    ChantierVue ref = ChantierVue.reference(ID, "Autoroute", T2);
    assertThat(ref.complet()).isFalse();
    assertThat(ref.montantMarche()).isEqualByComparingTo("0");
    ChantierVue complet = ref.fusionnerAvec(chantier("Autoroute A1", T1));
    // Un instantané complet remplace une référence même s'il est plus ancien.
    assertThat(complet.complet()).isTrue();
    assertThat(complet.fusionnerAvec(chantier("Ancien", T1.minusSeconds(60))).nom())
        .isEqualTo("Autoroute A1");
    assertThat(complet.fusionnerAvec(ChantierVue.reference(ID, "Autre", T2))).isSameAs(complet);
    ChantierVue sansNom = ChantierVue.reference(ID, null, T1);
    assertThat(sansNom.fusionnerAvec(ChantierVue.reference(ID, "Nom", T2)).nom()).isEqualTo("Nom");
    assertThat(sansNom.nomAffichable()).isEqualTo(ID.toString());
    assertThat(complet.intervalleMarche(365).orElseThrow().jours()).isEqualTo(365);
    assertThat(ref.intervalleMarche(365)).isEmpty();
  }

  @Test
  void should_considerer_chantier_actif_selon_dates_et_statut() {
    Periode octobre = new Periode(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    ChantierVue termine =
        new ChantierVue(
            ID,
            "CH",
            "N",
            null,
            StatutChantier.TERMINE,
            null,
            null,
            null,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 9, 30),
            true,
            T1);
    assertThat(termine.actifSur(octobre)).isFalse();
    ChantierVue futur =
        new ChantierVue(
            ID,
            "CH",
            "N",
            null,
            StatutChantier.EN_COURS,
            null,
            null,
            null,
            LocalDate.of(2026, 11, 1),
            null,
            true,
            T1);
    assertThat(futur.actifSur(octobre)).isFalse();
    assertThat(chantier("N", T1).actifSur(octobre)).isTrue();
  }

  @Test
  void should_ignorer_etat_plus_ancien_et_garder_reforme_terminal() {
    MaterielVue ref =
        MaterielVue.reference(ID, "M1", "Pelle", CategorieMateriel.MATERIEL_ROULANT, valueOf(100));
    MaterielVue affecte = ref.avecEtat(EtatMateriel.AFFECTE, UUID.randomUUID(), T2);
    MaterielVue enregistreAvant =
        new MaterielVue(
            ID,
            "M1",
            "Pelle CAT",
            null,
            EtatMateriel.DISPONIBLE,
            null,
            valueOf(0),
            valueOf(9),
            null,
            T1);
    MaterielVue fusion = affecte.fusionnerAvec(enregistreAvant);
    assertThat(fusion.etat()).isEqualTo(EtatMateriel.AFFECTE);
    assertThat(fusion.valeurAcquisition()).isEqualByComparingTo("9");
    assertThat(fusion.tarifJournalier()).isEqualByComparingTo("100");
    assertThat(affecte.avecEtat(EtatMateriel.DISPONIBLE, null, T1)).isSameAs(affecte);

    MaterielVue reforme = affecte.avecEtat(EtatMateriel.REFORME, null, T2.plusSeconds(1));
    assertThat(reforme.reforme()).isTrue();
    assertThat(reforme.avecEtat(EtatMateriel.DISPONIBLE, null, T2.plusSeconds(10)))
        .isSameAs(reforme);
    MaterielVue plusRecent =
        new MaterielVue(
            ID,
            null,
            null,
            null,
            EtatMateriel.DISPONIBLE,
            null,
            valueOf(200),
            null,
            null,
            T2.plusSeconds(20));
    assertThat(reforme.fusionnerAvec(plusRecent).etat()).isEqualTo(EtatMateriel.REFORME);
    assertThat(affecte.fusionnerAvec(plusRecent).tarifJournalier()).isEqualByComparingTo("200");
    assertThat(affecte.fusionnerAvec(plusRecent).code()).isEqualTo("M1");
  }

  @Test
  void should_traiter_alerte_de_maintenance_par_une_maintenance_posterieure() {
    MaterielVue m = MaterielVue.reference(ID, "M", "D", null, null).avecAlerte(T1);
    assertThat(m.alerteEnAttente()).isTrue();
    assertThat(m.alerteTraiteeLe(T1.minusSeconds(1)).alerteEnAttente()).isTrue();
    assertThat(m.alerteTraiteeLe(T2).alerteEnAttente()).isFalse();
    assertThat(
            MaterielVue.reference(ID, "M", "D", null, null)
                .alerteTraiteeLe(T2)
                .alerteMaintenanceLe())
        .isNull();
  }

  @Test
  void should_compter_jours_affectes_jusqu_a_aujourdhui() {
    AffectationVue enCours =
        new AffectationVue(
            ID,
            ID,
            null,
            null,
            valueOf(1000),
            null,
            LocalDate.of(2026, 10, 21),
            LocalDate.of(2026, 12, 31),
            null,
            StatutAffectation.EN_COURS,
            T1);
    Periode octobre = new Periode(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    assertThat(enCours.joursDans(octobre, AUJ)).isEqualTo(10);
    assertThat(enCours.coutDans(octobre, AUJ)).isEqualByComparingTo("10000");
    AffectationVue termineeSansReelle =
        new AffectationVue(
            ID,
            ID,
            null,
            null,
            null,
            null,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 3),
            null,
            StatutAffectation.TERMINEE,
            T1);
    assertThat(termineeSansReelle.joursDans(octobre, AUJ)).isEqualTo(3);
    AffectationVue future =
        new AffectationVue(
            ID,
            ID,
            null,
            null,
            null,
            null,
            LocalDate.of(2026, 11, 2),
            null,
            null,
            StatutAffectation.EN_ATTENTE_RECEPTION,
            T1);
    assertThat(future.joursOccupes(AUJ)).isEmpty();
    AffectationVue sansDebut =
        new AffectationVue(ID, ID, null, null, null, null, null, null, null, null, T1);
    assertThat(sansDebut.joursOccupes(AUJ)).isEmpty();
    // Même horodatage : l'instantané reçu l'emporte (deux événements d'une même transaction).
    assertThat(enCours.fusionnerAvec(sansDebut)).isSameAs(sansDebut);
  }

  @Test
  void should_imputer_cout_de_maintenance_a_la_date_de_fin() {
    MaintenanceVue cloturee =
        new MaintenanceVue(
            ID,
            "OT",
            ID,
            null,
            null,
            StatutMaintenance.CLOTUREE,
            valueOf(500),
            LocalDate.of(2026, 9, 28),
            LocalDate.of(2026, 9, 29),
            LocalDate.of(2026, 10, 2),
            T2,
            T2);
    Periode octobre = new Periode(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    assertThat(cloturee.coutDans(octobre)).isEqualByComparingTo("500");
    assertThat(cloturee.immobilisation(AUJ).orElseThrow().jours()).isEqualTo(4);
    MaintenanceVue planifiee =
        new MaintenanceVue(
            ID,
            "OT",
            ID,
            null,
            null,
            StatutMaintenance.PLANIFIEE,
            null,
            LocalDate.of(2026, 10, 5),
            null,
            null,
            null,
            T1);
    assertThat(planifiee.coutDans(octobre)).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(planifiee.immobilisation(AUJ)).isEmpty();
    MaintenanceVue sansFin =
        new MaintenanceVue(
            ID,
            "OT",
            ID,
            null,
            null,
            StatutMaintenance.CLOTUREE,
            valueOf(1),
            null,
            LocalDate.of(2026, 10, 5),
            null,
            null,
            T1);
    assertThat(sansFin.immobilisation(AUJ).orElseThrow().jours()).isEqualTo(1);
    // Un instantané plus ancien n'écrase pas la clôture, mais l'instant de clôture est conservé.
    MaintenanceVue fusion = cloturee.fusionnerAvec(planifiee);
    assertThat(fusion.statut()).isEqualTo(StatutMaintenance.CLOTUREE);
    assertThat(fusion.clotureeLe()).isEqualTo(T2);
    assertThat(planifiee.fusionnerAvec(cloturee).clotureeLe()).isEqualTo(T2);
  }

  @Test
  void should_resoudre_panne_par_cloture_posterieure() {
    PanneVue panne = new PanneVue(ID, ID, null, GravitePanne.MAJEURE, T2, null);
    assertThat(panne.resolueSi(T1)).isSameAs(panne);
    assertThat(panne.resolueSi(null)).isSameAs(panne);
    PanneVue resolue = panne.resolueSi(T2.plusSeconds(5));
    assertThat(resolue.ouverte()).isFalse();
    assertThat(resolue.resolueSi(T2.plusSeconds(50))).isSameAs(resolue);
  }

  @Test
  void should_garder_premiere_soumission_et_premiere_decision_de_besoin() {
    BesoinVue soumis =
        new BesoinVue(
            ID,
            "B",
            null,
            null,
            null,
            1,
            null,
            StatutBesoin.EN_ATTENTE,
            LocalDate.of(2026, 12, 1),
            T1,
            null,
            T1,
            T1);
    BesoinVue valide =
        new BesoinVue(
            ID,
            "B",
            null,
            null,
            null,
            1,
            null,
            StatutBesoin.VALIDE,
            LocalDate.of(2026, 12, 1),
            null,
            T2,
            T2,
            T2);
    BesoinVue enAchat =
        new BesoinVue(
            ID,
            "B",
            null,
            null,
            null,
            1,
            null,
            StatutBesoin.EN_ACHAT,
            LocalDate.of(2026, 12, 1),
            null,
            T2.plusSeconds(3600),
            T2.plusSeconds(3600),
            T2.plusSeconds(3600));
    BesoinVue fusion = soumis.fusionnerAvec(enAchat).fusionnerAvec(valide);
    assertThat(fusion.statut()).isEqualTo(StatutBesoin.EN_ACHAT);
    assertThat(fusion.soumisLe()).isEqualTo(T1);
    assertThat(fusion.decideLe()).isEqualTo(T2);
    assertThat(fusion.delaiTraitementHeures())
        .hasValueSatisfying(h -> assertThat(h).isEqualByComparingTo("24.0"));
    assertThat(fusion.dateSoumission()).contains(LocalDate.of(2026, 10, 1));
    assertThat(valide.delaiTraitementHeures()).isEmpty();
    // Besoin historique : date du besoin antérieure à la publication de l'événement.
    BesoinVue historique =
        new BesoinVue(
            ID,
            "B",
            null,
            null,
            null,
            1,
            null,
            StatutBesoin.EN_ATTENTE,
            LocalDate.of(2026, 3, 1),
            T1,
            null,
            T1,
            T1);
    assertThat(historique.dateSoumission()).contains(LocalDate.of(2026, 3, 1));
  }

  @Test
  void should_dater_publication_ao_et_engagement_contrat() {
    AppelOffreVue ao =
        new AppelOffreVue(
            ID,
            "AO",
            null,
            null,
            null,
            StatutAppelOffre.PUBLIE,
            T2,
            Instant.parse("2026-11-01T00:00:00Z"),
            null,
            null,
            T2);
    AppelOffreVue attribue =
        new AppelOffreVue(
            ID,
            "AO",
            null,
            null,
            null,
            StatutAppelOffre.ATTRIBUE,
            T2.plusSeconds(99),
            Instant.parse("2026-11-01T00:00:00Z"),
            valueOf(5),
            null,
            T2.plusSeconds(99));
    AppelOffreVue fusion = ao.fusionnerAvec(attribue);
    assertThat(fusion.statut()).isEqualTo(StatutAppelOffre.ATTRIBUE);
    assertThat(fusion.datePublication()).isEqualTo(LocalDate.of(2026, 10, 2));
    AppelOffreVue historique =
        new AppelOffreVue(
            ID,
            "AO",
            null,
            null,
            null,
            StatutAppelOffre.ATTRIBUE,
            T2,
            Instant.parse("2026-05-01T00:00:00Z"),
            null,
            null,
            T2);
    assertThat(historique.datePublication()).isEqualTo(LocalDate.of(2026, 5, 1));

    LocalDate signature = LocalDate.of(2026, 9, 1);
    assertThat(ContratVue.dateEngagementDeduite(StatutContrat.SIGNE, signature, T1)).isNull();
    assertThat(ContratVue.dateEngagementDeduite(StatutContrat.LIVRE, signature, T1))
        .isEqualTo(signature);
    assertThat(ContratVue.dateEngagementDeduite(StatutContrat.ENGAGEMENT_VALIDE, null, T1))
        .isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(ContratVue.dateEngagementDeduite(null, signature, T1)).isNull();
  }

  @Test
  void should_suivre_livraison_et_retard_de_contrat() {
    ContratVue engage =
        new ContratVue(
            ID,
            "C",
            null,
            null,
            null,
            null,
            null,
            StatutContrat.ENGAGEMENT_VALIDE,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 10, 10),
            null,
            T1);
    assertThat(engage.enRetard(AUJ)).isTrue();
    assertThat(engage.enCours()).isTrue();
    assertThat(engage.montant()).isEqualByComparingTo("0");
    ContratVue livre =
        new ContratVue(
            ID,
            "C",
            null,
            null,
            null,
            null,
            valueOf(10),
            StatutContrat.LIVRE,
            LocalDate.of(2026, 9, 1),
            null,
            LocalDate.of(2026, 10, 10),
            LocalDate.of(2026, 10, 9),
            T2);
    ContratVue fusion = engage.fusionnerAvec(livre);
    assertThat(fusion.dateEngagement()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(fusion.livreALHeure()).isTrue();
    assertThat(fusion.enRetard(AUJ)).isFalse();
    assertThat(fusion.enCours()).isFalse();
    assertThat(livre.fusionnerAvec(engage).livre()).isTrue();
    ContratVue sansPrevue =
        new ContratVue(
            ID,
            "C",
            null,
            null,
            null,
            null,
            null,
            StatutContrat.LIVRE,
            null,
            null,
            null,
            LocalDate.of(2026, 10, 9),
            T2);
    assertThat(sansPrevue.ponctualiteMesurable()).isFalse();
    assertThat(sansPrevue.livreALHeure()).isFalse();
  }

  @Test
  void should_dater_reception_mandatement_et_paiement_de_facture() {
    LocalDate[] recue = FactureVue.datesDeduites(StatutFacture.RECUE, DatesMetier.aucune(), T1);
    assertThat(recue[1]).isNull();
    LocalDate[] payee = FactureVue.datesDeduites(StatutFacture.PAYEE, DatesMetier.aucune(), T2);
    assertThat(payee[1]).isEqualTo(LocalDate.of(2026, 10, 2));
    FactureVue f1 =
        new FactureVue(
            ID,
            "F",
            null,
            null,
            valueOf(100),
            StatutFacture.RECUE,
            null,
            recue[0],
            recue[1],
            recue[2],
            T1);
    FactureVue f2 =
        new FactureVue(
            ID,
            "F",
            null,
            null,
            valueOf(100),
            StatutFacture.PAYEE,
            null,
            payee[0],
            payee[1],
            payee[2],
            T2);
    FactureVue fusion = f1.fusionnerAvec(f2);
    Periode octobre = new Periode(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    assertThat(fusion.dateReception()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(fusion.mandateeDans(octobre)).isTrue();
    assertThat(fusion.payeeDans(octobre)).isTrue();
    assertThat(f2.fusionnerAvec(f1).statut()).isEqualTo(StatutFacture.PAYEE);
  }

  @Test
  void should_dater_facture_par_dates_metier_when_payload_les_porte() {
    LocalDate[] payee =
        FactureVue.datesDeduites(
            StatutFacture.PAYEE,
            new DatesMetier(
                LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 20), LocalDate.of(2026, 6, 1)),
            T2);

    // Réception ramenée à l'émission, mandatement et paiement à leurs dates métier.
    assertThat(payee)
        .containsExactly(
            LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 20), LocalDate.of(2026, 6, 1));
    LocalDate[] mandatee =
        FactureVue.datesDeduites(
            StatutFacture.MANDATEE, new DatesMetier(null, LocalDate.of(2026, 9, 1), null), T2);
    assertThat(mandatee[0]).isEqualTo(LocalDate.of(2026, 10, 2));
    assertThat(mandatee[1]).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(mandatee[2]).isNull();
  }

  @Test
  void should_fusionner_fournisseur_et_litige() {
    FournisseurVue ref = FournisseurVue.reference(ID, "Sahel", T2);
    assertThat(ref.actif()).isFalse();
    FournisseurVue complet =
        ref.fusionnerAvec(
            new FournisseurVue(ID, "Sahel SA", StatutFournisseur.ACTIF, valueOf(80), true, T1));
    assertThat(complet.actif()).isTrue();
    assertThat(complet.fusionnerAvec(FournisseurVue.reference(ID, "X", T2))).isSameAs(complet);
    assertThat(FournisseurVue.reference(ID, null, T1).fusionnerAvec(ref).raisonSociale())
        .isEqualTo("Sahel");

    LitigeVue ouvert =
        new LitigeVue(ID, "L", ID, null, GraviteLitige.FAIBLE, StatutLitige.OUVERT, T1, T1);
    LitigeVue resolu =
        new LitigeVue(ID, "L", ID, null, GraviteLitige.FAIBLE, StatutLitige.RESOLU, T2, T2);
    LitigeVue fusion = resolu.fusionnerAvec(ouvert);
    assertThat(fusion.ouvert()).isFalse();
    assertThat(fusion.ouvertLe()).isEqualTo(T1);
    assertThat(new LitigeVue(ID, null, null, null, null, null, T1, T1).ouvert()).isTrue();
  }

  @Test
  void should_photographier_parc_par_categorie_et_chantier() {
    UUID chantier = UUID.randomUUID();
    List<MaterielVue> materiels =
        List.of(
            new MaterielVue(
                UUID.randomUUID(),
                "1",
                "a",
                CategorieMateriel.OUTILLAGE,
                EtatMateriel.DISPONIBLE,
                null,
                null,
                null,
                null,
                T1),
            new MaterielVue(
                UUID.randomUUID(),
                "2",
                "b",
                CategorieMateriel.OUTILLAGE,
                EtatMateriel.REFORME,
                null,
                null,
                null,
                null,
                T1),
            new MaterielVue(
                UUID.randomUUID(),
                "3",
                "c",
                CategorieMateriel.OUTILLAGE,
                EtatMateriel.AFFECTE,
                chantier,
                null,
                null,
                null,
                T1),
            MaterielVue.reference(UUID.randomUUID(), "4", "d", null, null));
    List<ParcInstantane> photo = ParcInstantane.photographier(AUJ, materiels);
    assertThat(photo).hasSize(2);
    ParcInstantane horsChantier =
        photo.stream().filter(p -> p.chantierId() == null).findFirst().orElseThrow();
    assertThat(horsChantier.total()).isEqualTo(2);
    assertThat(horsChantier.disponibles()).isEqualTo(1);
    assertThat(horsChantier.reformes()).isEqualTo(1);
  }

  @Test
  void should_ordonner_horodatages() {
    assertThat(Horodatage.plusRecent(T1, null)).isTrue();
    assertThat(Horodatage.plusRecent(null, T1)).isFalse();
    assertThat(Horodatage.plusRecent(T1, T1)).isTrue();
    assertThat(Horodatage.min(T2, T1)).isEqualTo(T1);
    assertThat(Horodatage.min((Instant) null, T1)).isEqualTo(T1);
    assertThat(Horodatage.max(T1, T2)).isEqualTo(T2);
    assertThat(Horodatage.max(null, T2)).isEqualTo(T2);
    assertThat(Horodatage.min(LocalDate.MAX, null)).isEqualTo(LocalDate.MAX);
    assertThat(Horodatage.jour(null)).isNull();
  }
}
