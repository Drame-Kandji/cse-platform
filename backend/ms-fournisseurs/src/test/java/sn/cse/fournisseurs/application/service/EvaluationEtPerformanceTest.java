package sn.cse.fournisseurs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.command.EvaluationCommand;
import sn.cse.fournisseurs.application.command.LivraisonCommand;
import sn.cse.fournisseurs.domain.model.Evaluation;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.support.Fixtures;

class EvaluationEtPerformanceTest {

  private final Contexte ctx = new Contexte();

  @Test
  void should_recalculer_le_score_when_nouvelle_evaluation_R07() {
    UUID id = ctx.enregistrer(Fixtures.actif()).getId();
    Evaluation e =
        ctx.evaluation.evaluer(
            id, new EvaluationCommand(null, 8, 7, 9, 6, 7, "RAS"), Fixtures.rm());
    assertThat(e.noteGlobale()).isEqualByComparingTo("76.0");
    assertThat(e.evaluateur().nom()).isEqualTo("Ibrahima Fall");
    assertThat(e.date()).isEqualTo(Fixtures.MAINTENANT);
    Fournisseur f = ctx.fournisseurs.parId(id).orElseThrow();
    assertThat(f.getPerformance().scorePerformance()).isEqualByComparingTo("76.0");
    assertThat(f.getPerformance().nombreEvaluations()).isEqualTo(1);
    assertThat(ctx.publications.types()).containsExactly(EventTypes.SCORE_RECALCULE);
    assertThat(ctx.evaluation.evaluations(id)).containsExactly(e);
  }

  @Test
  void should_signaler_le_seuil_dans_le_motif_when_score_faible_R07() {
    UUID id = ctx.enregistrer(Fixtures.actif()).getId();
    ctx.evaluation.evaluer(id, new EvaluationCommand(null, 2, 2, 2, 2, 2, null), Fixtures.ra());
    assertThat(ctx.publications.evenements.get(0).motif())
        .contains("inférieur au seuil d'alerte 40");
  }

  @Test
  void should_ne_rien_publier_when_score_inchange_R07() {
    UUID id = ctx.enregistrer(Fixtures.actif()).getId();
    ctx.performance.receptionConfirmee(id);
    assertThat(ctx.publications.evenements).isEmpty();
  }

  @Test
  void should_refuser_evaluation_when_en_attente_validation() {
    UUID id = ctx.enregistrer(Fixtures.enAttente()).getId();
    assertThatThrownBy(
            () ->
                ctx.evaluation.evaluer(
                    id, new EvaluationCommand(null, 5, 5, 5, 5, 5, null), Fixtures.ra()))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(ctx.evaluations.donnees).isEmpty();
  }

  @Test
  void should_integrer_la_ponctualite_when_livraison_effectuee_R07() {
    UUID id = ctx.enregistrer(Fixtures.actif()).getId();
    ctx.evaluation.evaluer(
        id, new EvaluationCommand(null, 10, 10, 10, 10, 10, null), Fixtures.ra());
    LocalDate prevue = LocalDate.of(2026, 9, 1);
    ctx.performance.enregistrerLivraison(
        new LivraisonCommand(id, "CTR-2026-001", prevue, prevue.plusDays(3)));
    Fournisseur f = ctx.fournisseurs.parId(id).orElseThrow();
    assertThat(f.getPerformance().tauxLivraisonALHeure())
        .get()
        .isEqualTo(new java.math.BigDecimal("0.0"));
    // 100 × 0,8 + 0 × 0,2 = 80
    assertThat(f.getPerformance().scorePerformance()).isEqualByComparingTo("80.0");
    assertThat(ctx.publications.evenements.get(1).motif())
        .isEqualTo("Livraison du contrat CTR-2026-001");
  }

  @Test
  void should_ignorer_evenement_d_un_fournisseur_inconnu() {
    ctx.performance.enregistrerLivraison(
        new LivraisonCommand(UUID.randomUUID(), "CTR", LocalDate.EPOCH, LocalDate.EPOCH));
    ctx.performance.receptionConfirmee(null);
    assertThat(ctx.publications.evenements).isEmpty();
  }
}
