package sn.cse.suivi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Coordonnees;
import sn.cse.suivi.domain.model.DescriptionMateriel;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.Periode;
import sn.cse.suivi.domain.model.TypeMouvement;

class ValeursTest {

  private static final LocalDate J = Fixtures.AUJOURDHUI;

  @Test
  void should_detect_overlapping_periods_inclusively() {
    Periode p = new Periode(J, J.plusDays(10));

    assertThat(p.chevauche(new Periode(J.plusDays(10), J.plusDays(20)))).isTrue();
    assertThat(p.chevauche(new Periode(J.minusDays(5), J))).isTrue();
    assertThat(p.chevauche(new Periode(J.plusDays(11), J.plusDays(20)))).isFalse();
    assertThat(p.chevauche(new Periode(J.minusDays(5), J.minusDays(1)))).isFalse();
    assertThat(p.dureeJours()).isEqualTo(11);
  }

  @Test
  void should_reject_period_when_end_before_start() {
    assertThatThrownBy(() -> new Periode(J, J.minusDays(1)))
        .isInstanceOf(BusinessRuleException.class)
        .extracting("code")
        .isEqualTo(Periode.PERIODE_INVALIDE);
  }

  @Test
  void should_validate_gps_bounds() {
    assertThat(Coordonnees.depuis(14.7, -17.4)).isEqualTo(new Coordonnees(14.7, -17.4));
    assertThat(Coordonnees.depuis(null, -17.4)).isNull();
    assertThatThrownBy(() -> new Coordonnees(91, 0)).isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> new Coordonnees(0, -181)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_reject_negative_amounts_in_description() {
    assertThatThrownBy(
            () ->
                new DescriptionMateriel(
                    "X",
                    null,
                    "X",
                    CategorieMateriel.OUTILLAGE,
                    null,
                    null,
                    null,
                    java.math.BigDecimal.valueOf(-1),
                    null,
                    null))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                new DescriptionMateriel(
                    "X",
                    null,
                    "X",
                    CategorieMateriel.OUTILLAGE,
                    null,
                    null,
                    null,
                    java.math.BigDecimal.ONE,
                    java.math.BigDecimal.valueOf(-5),
                    null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_reject_non_positive_maintenance_thresholds() {
    assertThatThrownBy(
            () -> new DonneesTypeEngin("T", "T", CategorieMateriel.OUTILLAGE, 0, null, true))
        .isInstanceOf(BusinessRuleException.class)
        .extracting("code")
        .isEqualTo("SEUIL_INVALIDE");
  }

  @Test
  void should_freeze_site_names_in_movement() {
    Chantier chantier = Fixtures.chantier();
    UUID materiel = UUID.randomUUID();

    Mouvement m =
        Mouvement.de(
            materiel,
            TypeMouvement.AFFECTATION,
            null,
            chantier,
            Instant.parse("2026-10-30T08:00:00Z"),
            new Mouvement.Auteur(null, "Système"),
            "Affectation");

    assertThat(m.chantierDestinationNom()).isEqualTo(chantier.getNom());
    assertThat(m.chantierDestinationId()).isEqualTo(chantier.getId());
    assertThat(m.chantierOrigineNom()).isNull();
  }
}
