package sn.cse.fournisseurs.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import sn.cse.fournisseurs.domain.model.NotesEvaluation;
import sn.cse.fournisseurs.support.Fixtures;

class ScorePondereTest {

  private final ScorePolicy politique = Fixtures.politique();
  private static final NotesEvaluation MOYENNE = new NotesEvaluation(8, 7, 9, 6, 7);
  private static final NotesEvaluation PARFAITE = new NotesEvaluation(10, 10, 10, 10, 10);

  @Test
  void should_ponderer_les_criteres_pour_la_note_globale_R07() {
    // 30×8 + 25×7 + 20×9 + 10×6 + 15×7 = 760 → 76,0 / 100
    assertThat(politique.noteGlobale(MOYENNE)).isEqualByComparingTo("76.0");
    assertThat(politique.noteGlobale(PARFAITE)).isEqualByComparingTo("100.0");
    assertThat(politique.noteGlobale(new NotesEvaluation(0, 0, 0, 0, 0)))
        .isEqualByComparingTo("0.0");
  }

  @Test
  void should_utiliser_le_score_initial_when_aucune_evaluation_R07() {
    BigDecimal score =
        politique.calculer(new IndicateursPerformance(List.of(), Optional.empty(), 0, 0));
    assertThat(score).isEqualByComparingTo("50.0");
    assertThat(politique.scoreInitial()).isEqualByComparingTo("50.0");
  }

  @Test
  void should_combiner_evaluations_ponctualite_et_litiges_R07() {
    // moyenne (76 + 100) / 2 = 88 ; 88 × 0,8 + 50 × 0,2 = 80,4 ; − 5 (ouvert) − 2 (résolu) = 73,4
    BigDecimal score =
        politique.calculer(
            new IndicateursPerformance(
                List.of(MOYENNE, PARFAITE), Optional.of(BigDecimal.valueOf(50)), 1, 1));
    assertThat(score).isEqualByComparingTo("73.4");
    assertThat(score.scale()).isEqualTo(1);
  }

  @Test
  void should_ignorer_la_ponctualite_when_aucune_livraison_R07() {
    BigDecimal score = politique.calculer(new IndicateursPerformance(List.of(MOYENNE), null, 0, 0));
    assertThat(score).isEqualByComparingTo("76.0");
  }

  @Test
  void should_borner_le_score_entre_0_et_100_R07() {
    assertThat(politique.calculer(new IndicateursPerformance(List.of(), Optional.empty(), 20, 0)))
        .isEqualByComparingTo("0.0");
    assertThat(
            politique.calculer(
                new IndicateursPerformance(
                    List.of(PARFAITE), Optional.of(BigDecimal.valueOf(100)), 0, 0)))
        .isEqualByComparingTo("100.0");
  }

  @Test
  void should_detecter_score_sous_le_seuil() {
    assertThat(politique.estSousSeuilAlerte(new BigDecimal("39.9"))).isTrue();
    assertThat(politique.estSousSeuilAlerte(new BigDecimal("40.0"))).isFalse();
    assertThat(politique.seuilAlerte()).isEqualByComparingTo("40");
  }

  @Test
  void should_appliquer_des_poids_configures_differents() {
    ParametresScore qualiteSeule =
        new ParametresScore(
            1, 0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE);
    ScorePolicy p = new ScorePondere(qualiteSeule);
    assertThat(p.noteGlobale(new NotesEvaluation(7, 0, 0, 0, 0))).isEqualByComparingTo("70.0");
    assertThat(
            p.calculer(
                new IndicateursPerformance(
                    List.of(new NotesEvaluation(7, 0, 0, 0, 0)),
                    Optional.of(BigDecimal.ZERO),
                    0,
                    0)))
        .isEqualByComparingTo("70.0");
  }

  @Test
  void should_refuser_parametres_incoherents() {
    assertThatThrownBy(
            () ->
                new ParametresScore(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new ParametresScore(
                    -1,
                    1,
                    0,
                    0,
                    0,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new ParametresScore(
                    1,
                    1,
                    0,
                    0,
                    0,
                    101,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ScorePondere(null)).isInstanceOf(NullPointerException.class);
  }
}
