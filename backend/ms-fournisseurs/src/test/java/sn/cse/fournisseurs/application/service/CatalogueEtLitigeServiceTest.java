package sn.cse.fournisseurs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.command.LigneCatalogueCommand;
import sn.cse.fournisseurs.application.command.LitigeCommand;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.GraviteLitige;
import sn.cse.fournisseurs.domain.model.Litige;
import sn.cse.fournisseurs.domain.model.StatutLitige;
import sn.cse.fournisseurs.domain.model.TypeActionLitige;
import sn.cse.fournisseurs.support.Fixtures;

class CatalogueEtLitigeServiceTest {

  private final Contexte ctx = new Contexte();

  private static LigneCatalogueCommand ligne(String ref, String designation, long prix) {
    return new LigneCatalogueCommand(
        ref, designation, CategorieMateriel.OUTILLAGE, BigDecimal.valueOf(prix), "unité", 10);
  }

  @Test
  void should_historiser_le_prix_initial_et_chaque_changement() {
    UUID id =
        ctx.enregistrer(Fixtures.actif("Dakar Outillage Pro", "001", CategorieMateriel.OUTILLAGE))
            .getId();
    ctx.catalogues.publierLigne(id, ligne("dop-ge100", "Groupe électrogène 100 kVA", 14_200_000));
    ctx.catalogues.publierLigne(id, ligne("DOP-GE100", "Groupe électrogène 100 kVA", 14_200_000));
    ctx.catalogues.publierLigne(id, ligne("DOP-GE100", "Groupe électrogène 100 kVA", 14_800_000));
    assertThat(ctx.catalogues.catalogue(id))
        .singleElement()
        .satisfies(
            l -> {
              assertThat(l.getReference()).isEqualTo("DOP-GE100");
              assertThat(l.getPrixUnitaire()).isEqualByComparingTo("14800000");
            });
    assertThat(ctx.catalogue.historique).hasSize(2);
    assertThat(ctx.catalogues.historique(id, " dop-ge100 ")).hasSize(2);
    assertThat(ctx.catalogues.historique(id, null)).hasSize(2);
    assertThat(ctx.catalogues.historique(id, "AUTRE")).isEmpty();
  }

  @Test
  void should_comparer_uniquement_les_fournisseurs_actifs() {
    UUID a = ctx.enregistrer(Fixtures.actif("A", "001", CategorieMateriel.OUTILLAGE)).getId();
    UUID b = ctx.enregistrer(Fixtures.actif("B", "002", CategorieMateriel.OUTILLAGE)).getId();
    UUID c = ctx.enregistrer(Fixtures.actif("C", "003", CategorieMateriel.OUTILLAGE)).getId();
    ctx.catalogues.publierLigne(a, ligne("A1", "Groupe électrogène 100 kVA", 15_000_000));
    ctx.catalogues.publierLigne(b, ligne("B1", "Groupe electrogene 100 kVA", 14_000_000));
    ctx.catalogues.publierLigne(c, ligne("C1", "Groupe électrogène 100 kVA", 1_000));
    ctx.cycleVie.suspendre(c, "motif", Fixtures.ra());
    var comparaisons = ctx.catalogues.comparer(CategorieMateriel.OUTILLAGE, " groupe ");
    assertThat(comparaisons)
        .singleElement()
        .satisfies(
            cp -> {
              assertThat(cp.prixMin()).isEqualByComparingTo("14000000");
              assertThat(cp.offres()).hasSize(2);
            });
    assertThat(ctx.catalogues.comparer(null, null)).hasSize(1);
  }

  @Test
  void should_renvoyer_aucune_comparaison_sans_actif() {
    assertThat(ctx.catalogues.comparer(null, null)).isEmpty();
  }

  @Test
  void should_lever_404_sur_catalogue_inconnu() {
    assertThatThrownBy(() -> ctx.catalogues.catalogue(UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_ouvrir_litige_avec_reference_et_penaliser_le_score_R07() {
    Fournisseur f = ctx.enregistrer(Fixtures.actif());
    Litige l =
        ctx.litigeService.ouvrir(
            f.getId(),
            new LitigeCommand(null, "Livraison incomplète", "2 sur 5", GraviteLitige.MOYENNE),
            Fixtures.rm());
    assertThat(l.getReference()).isEqualTo("LIT-2026-001");
    assertThat(l.getStatut()).isEqualTo(StatutLitige.OUVERT);
    assertThat(ctx.publications.types())
        .containsExactly(EventTypes.LITIGE_OUVERT, EventTypes.SCORE_RECALCULE);
    assertThat(ctx.fournisseurs.parId(f.getId()).orElseThrow().getPerformance().scorePerformance())
        .isEqualByComparingTo("45.0");
  }

  @Test
  void should_recalculer_le_score_when_litige_resolu_R07() {
    Fournisseur f = ctx.enregistrer(Fixtures.actif());
    Litige l =
        ctx.litigeService.ouvrir(
            f.getId(),
            new LitigeCommand(null, "Objet", "Description", GraviteLitige.ELEVEE),
            Fixtures.ra());
    ctx.litigeService.agir(l.getId(), TypeActionLitige.ACTION, "Relance", Fixtures.ra());
    assertThat(ctx.publications.types()).hasSize(2);
    Litige resolu =
        ctx.litigeService.agir(
            l.getId(), TypeActionLitige.RESOLUTION, "Avoir accepté", Fixtures.ra());
    assertThat(resolu.getStatut()).isEqualTo(StatutLitige.RESOLU);
    assertThat(ctx.publications.types())
        .containsExactly(
            EventTypes.LITIGE_OUVERT,
            EventTypes.SCORE_RECALCULE,
            EventTypes.LITIGE_RESOLU,
            EventTypes.SCORE_RECALCULE);
    // 50 − 2 (litige résolu)
    assertThat(ctx.fournisseurs.parId(f.getId()).orElseThrow().getPerformance().scorePerformance())
        .isEqualByComparingTo("48.0");
    assertThat(
            ctx.litigeService
                .rechercher(
                    StatutLitige.RESOLU, f.getId(), new PageQuery(0, 20, "dateOuverture", false))
                .content())
        .hasSize(1);
  }

  @Test
  void should_lever_404_sur_litige_inconnu() {
    assertThatThrownBy(
            () ->
                ctx.litigeService.agir(
                    UUID.randomUUID(), TypeActionLitige.ACTION, "x", Fixtures.ra()))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
