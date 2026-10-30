package sn.cse.besoins.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import sn.cse.common.exception.BusinessRuleException;

class DescriptionBesoinTest {

  private static final LocalDate DATE = LocalDate.parse("2026-11-15");

  private static DescriptionBesoin avec(Priorite priorite, String justification, int quantite) {
    return new DescriptionBesoin(
        "  Niveleuse 140 CV ",
        CategorieMateriel.MATERIEL_ROULANT,
        null,
        quantite,
        " ",
        priorite,
        10,
        DATE,
        justification);
  }

  @ParameterizedTest
  @EnumSource(
      value = Priorite.class,
      names = {"URGENT", "CRITIQUE"})
  void should_refuser_sans_justification_when_priorite_elevee_R08(Priorite priorite) {
    assertThatThrownBy(() -> avec(priorite, "   ", 1))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", "JUSTIFICATION_OBLIGATOIRE");
  }

  @Test
  void should_accepter_sans_justification_when_priorite_normale_R08() {
    DescriptionBesoin d = avec(Priorite.NORMAL, null, 1);
    assertThat(d.justification()).isNull();
    assertThat(d.designation()).isEqualTo("Niveleuse 140 CV");
    assertThat(d.specifications()).isNull();
    assertThat(d.dateFinPrevue()).isEqualTo(DATE.plusDays(10));
  }

  @Test
  void should_accepter_when_priorite_urgente_justifiee_R08() {
    assertThat(avec(Priorite.URGENT, "Panne de l'engin principal", 1).justification())
        .isEqualTo("Panne de l'engin principal");
  }

  @Test
  void should_refuser_quantite_nulle_duree_nulle_ou_designation_vide() {
    assertThatThrownBy(() -> avec(Priorite.NORMAL, null, 0))
        .hasFieldOrPropertyWithValue("code", "BESOIN_INVALIDE");
    assertThatThrownBy(
            () ->
                new DescriptionBesoin(
                    "Pelle",
                    CategorieMateriel.OUTILLAGE,
                    null,
                    1,
                    null,
                    Priorite.NORMAL,
                    0,
                    DATE,
                    null))
        .hasFieldOrPropertyWithValue("code", "BESOIN_INVALIDE");
    assertThatThrownBy(
            () ->
                new DescriptionBesoin(
                    " ",
                    CategorieMateriel.OUTILLAGE,
                    null,
                    1,
                    null,
                    Priorite.NORMAL,
                    3,
                    DATE,
                    null))
        .hasFieldOrPropertyWithValue("code", "BESOIN_INVALIDE");
  }
}
