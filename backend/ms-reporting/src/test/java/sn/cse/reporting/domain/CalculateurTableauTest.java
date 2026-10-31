package sn.cse.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static sn.cse.reporting.domain.JeuDeDonnees.AUJOURDHUI;
import static sn.cse.reporting.domain.JeuDeDonnees.C1;
import static sn.cse.reporting.domain.JeuDeDonnees.C2;
import static sn.cse.reporting.domain.JeuDeDonnees.F1;
import static sn.cse.reporting.domain.JeuDeDonnees.MAINTENANT;
import static sn.cse.reporting.domain.JeuDeDonnees.OCTOBRE;

import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.DonneesReporting;
import sn.cse.reporting.domain.model.ParametresReporting;
import sn.cse.reporting.domain.model.Perimetre;
import sn.cse.reporting.domain.model.Priorite;
import sn.cse.reporting.domain.model.ProfilTableau;
import sn.cse.reporting.domain.model.Tableau;
import sn.cse.reporting.domain.service.CalculateurTableau;

/** Indicateurs calculés sur {@link JeuDeDonnees}, valeurs attendues établies à la main. */
class CalculateurTableauTest {

  private final CalculateurTableau calculateur =
      new CalculateurTableau(new ParametresReporting(90, 1830, 400, 5, 365));
  private final DonneesReporting donnees = JeuDeDonnees.donnees();

  private static BigDecimal d(String v) {
    return new BigDecimal(v);
  }

  private Tableau tableau(ProfilTableau profil, Perimetre perimetre) {
    return calculateur.calculer(profil, OCTOBRE, perimetre, donnees, AUJOURDHUI, MAINTENANT);
  }

  @Test
  void should_remplir_toutes_les_sections_when_profil_DG() {
    Tableau t = tableau(ProfilTableau.DG, Perimetre.tous());
    assertThat(t.parc()).isNotNull();
    assertThat(t.besoins()).isNotNull();
    assertThat(t.achats()).isNotNull();
    assertThat(t.fournisseurs()).isNotNull();
    assertThat(t.finances()).isNotNull();
    assertThat(t.chantiers()).isNotNull();
    assertThat(t.du()).isEqualTo(OCTOBRE.du());
    assertThat(t.genereLe()).isEqualTo(MAINTENANT);
  }

  @Test
  void should_ne_remplir_que_ses_sections_when_profil_restreint() {
    Tableau ra = tableau(ProfilTableau.RESPONSABLE_ACHATS, Perimetre.tous());
    assertThat(ra.achats()).isNotNull();
    assertThat(ra.fournisseurs()).isNotNull();
    assertThat(ra.parc()).isNull();
    assertThat(ra.besoins()).isNull();
    assertThat(ra.finances()).isNull();
    assertThat(ra.chantiers()).isNull();

    Tableau daf = tableau(ProfilTableau.DAF, Perimetre.tous());
    assertThat(daf.finances()).isNotNull();
    assertThat(daf.achats()).isNotNull();
    assertThat(daf.chantiers()).isNotNull();
    assertThat(daf.parc()).isNull();
    assertThat(daf.fournisseurs()).isNull();

    Tableau rm = tableau(ProfilTableau.RESPONSABLE_MATERIEL, Perimetre.tous());
    assertThat(rm.parc()).isNotNull();
    assertThat(rm.besoins()).isNotNull();
    assertThat(rm.chantiers()).isNotNull();
    assertThat(rm.achats()).isNull();
  }

  @Test
  void should_calculer_disponibilite_et_utilisation_du_parc() {
    Tableau.Parc p = tableau(ProfilTableau.DG, Perimetre.tous()).parc();

    assertThat(p.total()).isEqualTo(4);
    assertThat(p.disponibles()).isEqualTo(1);
    assertThat(p.affectes()).isEqualTo(1);
    assertThat(p.enMaintenance()).isEqualTo(1);
    assertThat(p.reformes()).isEqualTo(1);
    // 1 disponible / (4 − 1 réformé)
    assertThat(p.tauxDisponibilite()).isEqualByComparingTo(d("33.3"));
    // M1 20/30, M2 5/28 (2 j de maintenance), M3 20/20 (10 j immobilisé) → 45 / 78
    assertThat(p.tauxUtilisation()).isEqualByComparingTo(d("57.7"));
    assertThat(p.maintenancesEnCours()).isEqualTo(1);
    assertThat(p.pannesOuvertes()).isEqualTo(1);
    assertThat(p.alertesMaintenance()).isEqualTo(1);
    assertThat(p.parCategorie()).hasSize(4);
    Tableau.ParcCategorie roulant =
        p.parCategorie().stream()
            .filter(c -> c.categorie() == CategorieMateriel.MATERIEL_ROULANT)
            .findFirst()
            .orElseThrow();
    // M1 20/30 + M2 5/28 → 25 / 58
    assertThat(roulant.tauxUtilisation()).isEqualByComparingTo(d("43.1"));
    assertThat(roulant.total()).isEqualTo(2);
    Tableau.ParcCategorie outillage =
        p.parCategorie().stream()
            .filter(c -> c.categorie() == CategorieMateriel.OUTILLAGE)
            .findFirst()
            .orElseThrow();
    assertThat(outillage.tauxUtilisation()).isEqualByComparingTo(d("100.0"));
  }

  @Test
  void should_compter_besoins_soumis_sur_la_periode_et_delai_moyen() {
    Tableau.Besoins b = tableau(ProfilTableau.DG, Perimetre.tous()).besoins();

    assertThat(b.total()).isEqualTo(3);
    assertThat(b.enAttente()).isEqualTo(1);
    assertThat(b.affectesDirect()).isEqualTo(1);
    assertThat(b.rejetes()).isEqualTo(1);
    assertThat(b.enAchat()).isZero();
    // (12 h + 24 h) / 2
    assertThat(b.delaiMoyenTraitementHeures()).isEqualByComparingTo(d("18.0"));
    assertThat(b.parPriorite())
        .containsEntry(Priorite.NORMAL, 1L)
        .containsEntry(Priorite.URGENT, 1L)
        .containsEntry(Priorite.CRITIQUE, 1L);
  }

  @Test
  void should_calculer_delai_approvisionnement_et_ponctualite() {
    Tableau.Achats a = tableau(ProfilTableau.DG, Perimetre.tous()).achats();

    assertThat(a.aoPublies()).isEqualTo(1);
    assertThat(a.aoClotures()).isEqualTo(1);
    assertThat(a.aoAttribues()).isZero();
    assertThat(a.contratsEnCours()).isEqualTo(1);
    assertThat(a.livraisonsEnRetard()).isEqualTo(1);
    // K1 : 10/09 → 18/10 = 38 j ; K2 : 01/08 → 15/10 = 75 j
    assertThat(a.delaiMoyenApprovisionnementJours()).isEqualByComparingTo(d("56.5"));
    assertThat(a.tauxLivraisonALHeure()).isEqualByComparingTo(d("50.0"));
  }

  @Test
  void should_classer_fournisseurs_actifs_par_score() {
    Tableau.Fournisseurs f = tableau(ProfilTableau.DG, Perimetre.tous()).fournisseurs();

    assertThat(f.actifs()).isEqualTo(2);
    assertThat(f.scoreMoyen()).isEqualByComparingTo(d("70.0"));
    assertThat(f.litigesOuverts()).isEqualTo(1);
    assertThat(f.top()).hasSize(2);
    assertThat(f.top().get(0).fournisseurId()).isEqualTo(F1);
    assertThat(f.top().get(0).nombreLitiges()).isEqualTo(1);
    assertThat(f.top().get(0).tauxLivraisonALHeure()).isEqualByComparingTo(d("100.0"));
    assertThat(f.top().get(1).tauxLivraisonALHeure()).isEqualByComparingTo(d("0.0"));
  }

  @Test
  void should_calculer_budget_engage_mandate_paye_et_couts() {
    Tableau.Finances f = tableau(ProfilTableau.DG, Perimetre.tous()).finances();

    // C3 terminé fin août : hors période
    assertThat(f.budgetTotal()).isEqualByComparingTo(d("15000000"));
    // K1 (02/10) + K3 (25/10) ; K2 engagé en septembre
    assertThat(f.montantEngage()).isEqualByComparingTo(d("5000000"));
    assertThat(f.montantMandate()).isEqualByComparingTo(d("1500000"));
    assertThat(f.montantPaye()).isEqualByComparingTo(d("800000"));
    assertThat(f.tauxExecutionBudget()).isEqualByComparingTo(d("33.3"));
    assertThat(f.coutMaintenance()).isEqualByComparingTo(d("300000"));
    assertThat(f.coutExploitation()).isEqualByComparingTo(d("2550000"));
  }

  @Test
  void should_calculer_marge_par_chantier() {
    var chantiers = tableau(ProfilTableau.DG, Perimetre.tous()).chantiers();

    assertThat(chantiers).extracting(Tableau.Chantier::chantierId).containsExactly(C1, C2);
    Tableau.Chantier c1 = chantiers.get(0);
    // 36 500 000 × 30 / 365 − (2 000 000 + 300 000) − 0
    assertThat(c1.marge()).isEqualByComparingTo(d("700000"));
    assertThat(c1.coutExploitation()).isEqualByComparingTo(d("2300000"));
    assertThat(c1.montantEngage()).isEqualByComparingTo(d("5000000"));
    assertThat(c1.materielsAffectes()).isEqualTo(1);
    // M1 20/30 + M3 20/20
    assertThat(c1.tauxUtilisation()).isEqualByComparingTo(d("80.0"));
    assertThat(c1.besoinsEnAttente()).isEqualTo(1);
    Tableau.Chantier c2 = chantiers.get(1);
    assertThat(c2.marge()).isEqualByComparingTo(d("-250000"));
    assertThat(c2.tauxUtilisation()).isEqualByComparingTo(d("17.9"));
  }

  @Test
  void should_restreindre_aux_chantiers_du_chef() {
    Tableau t = tableau(ProfilTableau.CHEF_CHANTIER, Perimetre.chantiers(Set.of(C1)));

    assertThat(t.parc().total()).isEqualTo(2);
    assertThat(t.parc().tauxDisponibilite()).isEqualByComparingTo(d("0.0"));
    assertThat(t.parc().tauxUtilisation()).isEqualByComparingTo(d("80.0"));
    assertThat(t.parc().pannesOuvertes()).isEqualTo(1);
    assertThat(t.besoins().total()).isEqualTo(2);
    assertThat(t.chantiers()).extracting(Tableau.Chantier::chantierId).containsExactly(C1);
    assertThat(t.achats()).isNull();

    Tableau c2 = tableau(ProfilTableau.DAF, Perimetre.chantiers(Set.of(C2)));
    // Facture FA2 (K2, payée le 10/10) seule visible
    assertThat(c2.finances().montantPaye()).isEqualByComparingTo(d("800000"));
    assertThat(c2.finances().montantMandate()).isEqualByComparingTo(d("0"));
    assertThat(c2.finances().budgetTotal()).isEqualByComparingTo(d("5000000"));
  }

  @Test
  void should_renvoyer_indicateurs_vides_when_aucune_donnee() {
    Tableau t =
        calculateur.calculer(
            ProfilTableau.DG,
            OCTOBRE,
            Perimetre.tous(),
            DonneesReporting.vide(),
            AUJOURDHUI,
            MAINTENANT);
    assertThat(t.parc().tauxDisponibilite()).isEqualByComparingTo(d("0.0"));
    assertThat(t.parc().tauxUtilisation()).isEqualByComparingTo(d("0.0"));
    assertThat(t.besoins().delaiMoyenTraitementHeures()).isNull();
    assertThat(t.achats().delaiMoyenApprovisionnementJours()).isNull();
    assertThat(t.achats().tauxLivraisonALHeure()).isNull();
    assertThat(t.fournisseurs().scoreMoyen()).isEqualByComparingTo(d("0.0"));
    assertThat(t.finances().tauxExecutionBudget()).isEqualByComparingTo(d("0.0"));
    assertThat(t.chantiers()).isEmpty();
  }
}
