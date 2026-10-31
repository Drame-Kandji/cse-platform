package sn.cse.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sn.cse.notification.domain.Fixtures.evenement;

import java.util.Map;
import org.junit.jupiter.api.Test;
import sn.cse.notification.domain.model.ConditionRegle;
import sn.cse.notification.domain.model.EvenementMetier;
import sn.cse.notification.domain.model.Operateur;

class ConditionRegleTest {

  private final EvenementMetier evt =
      evenement(
          "SCORE_RECALCULE",
          Map.of(
              "scorePerformance",
              54.5,
              "approbationDgRequise",
              true,
              "statut",
              "ACTIF",
              "fournisseur",
              Map.of("ville", "Dakar"),
              "texte",
              "abc"));

  @Test
  void should_compare_numbers_when_inferieur_or_superieur() {
    assertThat(new ConditionRegle("scorePerformance", Operateur.INFERIEUR, "60").estSatisfaite(evt))
        .isTrue();
    assertThat(new ConditionRegle("scorePerformance", Operateur.INFERIEUR, "50").estSatisfaite(evt))
        .isFalse();
    assertThat(new ConditionRegle("scorePerformance", Operateur.SUPERIEUR, "50").estSatisfaite(evt))
        .isTrue();
    assertThat(new ConditionRegle("texte", Operateur.SUPERIEUR, "1").estSatisfaite(evt)).isFalse();
    assertThat(new ConditionRegle("absent", Operateur.INFERIEUR, "1").estSatisfaite(evt)).isFalse();
  }

  @Test
  void should_evaluate_booleans_and_presence() {
    assertThat(new ConditionRegle("approbationDgRequise", Operateur.VRAI, null).estSatisfaite(evt))
        .isTrue();
    assertThat(new ConditionRegle("approbationDgRequise", Operateur.FAUX, null).estSatisfaite(evt))
        .isFalse();
    assertThat(new ConditionRegle("absent", Operateur.FAUX, null).estSatisfaite(evt)).isTrue();
    assertThat(new ConditionRegle("absent", Operateur.VRAI, null).estSatisfaite(evt)).isFalse();
    assertThat(new ConditionRegle("statut", Operateur.PRESENT, null).estSatisfaite(evt)).isTrue();
    assertThat(new ConditionRegle("absent", Operateur.PRESENT, null).estSatisfaite(evt)).isFalse();
  }

  @Test
  void should_compare_text_case_insensitively_and_read_nested_paths() {
    assertThat(new ConditionRegle("statut", Operateur.EGAL, "actif").estSatisfaite(evt)).isTrue();
    assertThat(new ConditionRegle("fournisseur.ville", Operateur.EGAL, "DAKAR").estSatisfaite(evt))
        .isTrue();
    assertThat(new ConditionRegle("statut.x", Operateur.EGAL, "a").estSatisfaite(evt)).isFalse();
    assertThat(new ConditionRegle("absent", Operateur.DIFFERENT, "X").estSatisfaite(evt)).isTrue();
    assertThat(new ConditionRegle("absent", Operateur.EGAL, "X").estSatisfaite(evt)).isFalse();
    assertThat(new ConditionRegle("texte", Operateur.COMMENCE_PAR, "AB").estSatisfaite(evt))
        .isTrue();
    assertThat(new ConditionRegle("statut", Operateur.COMMENCE_PAR, "x").estSatisfaite(evt))
        .isFalse();
    assertThat(new ConditionRegle("absent", Operateur.COMMENCE_PAR, "x").estSatisfaite(evt))
        .isFalse();
  }

  @Test
  void should_reject_condition_without_value_when_operator_requires_one() {
    assertThatThrownBy(() -> new ConditionRegle("x", Operateur.EGAL, " "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
