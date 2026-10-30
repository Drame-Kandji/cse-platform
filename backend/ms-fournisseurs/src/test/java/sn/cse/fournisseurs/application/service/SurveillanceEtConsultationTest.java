package sn.cse.fournisseurs.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.pagination.PageQuery;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.command.LitigeCommand;
import sn.cse.fournisseurs.application.query.CriteresFournisseurs;
import sn.cse.fournisseurs.application.query.StatistiquesFournisseurs;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.GraviteLitige;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.domain.model.TypeDocument;
import sn.cse.fournisseurs.support.Fixtures;

class SurveillanceEtConsultationTest {

  private final Contexte ctx = new Contexte();

  @Test
  void should_signaler_une_seule_fois_les_documents_expires() {
    Fournisseur f = Fixtures.actif();
    f.ajouterDocument(TypeDocument.QUITUS_FISCAL, "Quitus", Fixtures.AUJOURDHUI.minusDays(3));
    f.ajouterDocument(TypeDocument.ASSURANCE, "Assurance", Fixtures.AUJOURDHUI.minusDays(1));
    f.ajouterDocument(TypeDocument.NINEA, "NINEA", null);
    ctx.enregistrer(f);
    assertThat(ctx.surveillance.signalerDocumentsExpires()).isEqualTo(2);
    assertThat(ctx.publications.types())
        .containsExactly(EventTypes.DOCUMENT_EXPIRE, EventTypes.DOCUMENT_EXPIRE);
    assertThat(ctx.publications.evenements.get(0).motif())
        .contains("QUITUS_FISCAL")
        .contains("2026-10-27");
    assertThat(ctx.surveillance.signalerDocumentsExpires()).isZero();
  }

  @Test
  void should_alerter_les_actifs_sous_le_seuil() {
    Fournisseur faible = Fixtures.actif("Faible", "001");
    faible.actualiserScore(new BigDecimal("35.0"), 2);
    Fournisseur bon = Fixtures.actif("Bon", "002");
    Fournisseur suspenduFaible = Fixtures.actif("Suspendu", "003");
    suspenduFaible.actualiserScore(new BigDecimal("10.0"), 1);
    suspenduFaible.suspendre("motif");
    ctx.enregistrer(faible);
    ctx.enregistrer(bon);
    ctx.enregistrer(suspenduFaible);
    assertThat(ctx.surveillance.alerterScoresFaibles()).isEqualTo(1);
    assertThat(ctx.publications.evenements)
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.type()).isEqualTo(EventTypes.SCORE_RECALCULE);
              assertThat(e.aggregateId()).isEqualTo(faible.getId());
              assertThat(e.motif()).startsWith("ALERTE : score 35.0");
            });
  }

  @Test
  void should_masquer_coordonnees_bancaires_sauf_ra_et_daf() {
    UUID id = ctx.enregistrer(Fixtures.actif()).getId();
    assertThat(ctx.consultation.detail(id, Fixtures.ra()).coordonneesBancairesVisibles()).isTrue();
    assertThat(ctx.consultation.detail(id, Fixtures.daf()).coordonneesBancairesVisibles()).isTrue();
    assertThat(ctx.consultation.detail(id, Fixtures.rm()).coordonneesBancairesVisibles()).isFalse();
    assertThat(ctx.consultation.detail(id, Fixtures.dg()).coordonneesBancairesVisibles()).isFalse();
    assertThat(ctx.consultation.detail(id, null).coordonneesBancairesVisibles()).isFalse();
  }

  @Test
  void should_filtrer_les_actifs_par_specialite_pour_achats_R14() {
    ctx.enregistrer(Fixtures.actif("Roulant actif", "001", CategorieMateriel.MATERIEL_ROULANT));
    ctx.enregistrer(Fixtures.actif("Outillage actif", "002", CategorieMateriel.OUTILLAGE));
    Fournisseur suspendu =
        Fixtures.actif("Roulant suspendu", "003", CategorieMateriel.MATERIEL_ROULANT);
    suspendu.suspendre("motif");
    ctx.enregistrer(suspendu);
    var page =
        ctx.consultation.rechercher(
            new CriteresFournisseurs(
                " ", CategorieMateriel.MATERIEL_ROULANT, StatutFournisseur.ACTIF),
            new PageQuery(0, 20, "raisonSociale", true));
    assertThat(page.content())
        .singleElement()
        .satisfies(
            v -> {
              assertThat(v.fournisseur().getProfil().raisonSociale()).isEqualTo("Roulant actif");
              assertThat(v.coordonneesBancairesVisibles()).isFalse();
            });
    assertThat(page.totalElements()).isEqualTo(1);
  }

  @Test
  void should_compter_litiges_et_produire_statistiques_et_classement() {
    Fournisseur a = ctx.enregistrer(Fixtures.actif("A", "001"));
    Fournisseur b = Fixtures.actif("B", "002");
    b.ajouterDocument(TypeDocument.ASSURANCE, "Assurance", Fixtures.AUJOURDHUI.minusDays(1));
    ctx.enregistrer(b);
    ctx.enregistrer(Fixtures.enAttente());
    ctx.litigeService.ouvrir(
        a.getId(),
        new LitigeCommand(null, "Objet", "Description", GraviteLitige.FAIBLE),
        Fixtures.ra());

    StatistiquesFournisseurs s = ctx.consultation.statistiques();
    assertThat(s.total()).isEqualTo(3);
    assertThat(s.actifs()).isEqualTo(2);
    assertThat(s.enAttente()).isEqualTo(1);
    assertThat(s.suspendus()).isZero();
    assertThat(s.blacklistes()).isZero();
    // A : 50 − 5 (litige ouvert) = 45 ; B : 50 → moyenne 47,5
    assertThat(s.scoreMoyen()).isEqualByComparingTo("47.5");
    assertThat(s.litigesOuverts()).isEqualTo(1);
    assertThat(s.documentsExpires()).isEqualTo(1);

    var vueA = ctx.consultation.detail(a.getId(), Fixtures.rm());
    assertThat(vueA.nombreLitigesOuverts()).isEqualTo(1);
    var classement = ctx.consultation.classement(null);
    assertThat(classement)
        .extracting(r -> r.fournisseur().getProfil().raisonSociale())
        .containsExactly("B", "A");
    assertThat(classement.get(1).nombreLitiges()).isEqualTo(1);
  }

  @Test
  void should_renvoyer_statistiques_vides() {
    StatistiquesFournisseurs s = ctx.consultation.statistiques();
    assertThat(s.total()).isZero();
    assertThat(s.scoreMoyen()).isEqualByComparingTo("0");
  }
}
