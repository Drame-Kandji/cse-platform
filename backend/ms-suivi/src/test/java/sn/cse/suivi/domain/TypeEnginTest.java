package sn.cse.suivi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.TypeEngin;

class TypeEnginTest {

  @Test
  void should_report_threshold_change_only_when_thresholds_differ() {
    TypeEngin type = Fixtures.typePelle(500, 180);

    boolean libelle =
        type.modifier(
            new DonneesTypeEngin(
                "PELLE_HYD", "Pelle", CategorieMateriel.MATERIEL_ROULANT, 500, 180, true));
    boolean seuil =
        type.modifier(
            new DonneesTypeEngin(
                "PELLE_HYD", "Pelle", CategorieMateriel.MATERIEL_ROULANT, 450, 180, true));

    assertThat(libelle).isFalse();
    assertThat(seuil).isTrue();
    assertThat(type.getLibelle()).isEqualTo("Pelle");
    assertThat(type.getCode()).isEqualTo("PELLE_HYD");
    assertThat(TypeEngin.reconstituer(type.snapshot()).getDonnees()).isEqualTo(type.getDonnees());
  }
}
