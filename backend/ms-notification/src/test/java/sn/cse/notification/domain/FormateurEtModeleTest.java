package sn.cse.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import sn.cse.notification.domain.service.FormateurValeurs;
import sn.cse.notification.domain.service.ModeleTexte;

class FormateurEtModeleTest {

  private final FormateurValeurs f =
      new FormateurValeurs(Map.of("MATERIEL_ROULANT", "matériel roulant"));

  @Test
  void should_format_values_in_french() {
    assertThat(f.formater(150_000_000)).isEqualTo("150 000 000");
    assertThat(f.formater(new BigDecimal("54.5"))).isEqualTo("54,5");
    assertThat(f.formater("2026-10-30")).isEqualTo("30/10/2026");
    assertThat(f.formater("2026-10-30T09:15:00Z")).isEqualTo("30/10/2026 09:15");
    assertThat(f.formater("2026-13-45")).isEqualTo("2026-13-45");
    assertThat(f.formater("2026-13-45T99:00:00Z")).isEqualTo("2026-13-45T99:00:00Z");
    assertThat(f.formater("MATERIEL_ROULANT")).isEqualTo("matériel roulant");
    assertThat(f.formater(List.of("MATERIEL_ROULANT", "OUTILLAGE")))
        .isEqualTo("matériel roulant, OUTILLAGE");
    assertThat(f.formater(null)).isEmpty();
    assertThat(new FormateurValeurs(null).formater("X")).isEqualTo("X");
  }

  @Test
  void should_resolve_placeholders_and_report_missing_ones() {
    ModeleTexte.Resolution r =
        ModeleTexte.resoudre(
            "Besoin {reference} {absent}  ok",
            c -> c.equals("reference") ? Optional.of("BES-1") : Optional.empty(),
            "?");
    assertThat(r.texte()).isEqualTo("Besoin BES-1 ? ok");
    assertThat(r.complet()).isFalse();
    assertThat(ModeleTexte.resoudre("/x/{id}", c -> Optional.of("$1"), "").texte())
        .isEqualTo("/x/$1");
    assertThat(ModeleTexte.resoudre(null, c -> Optional.empty(), "")).isNull();
  }
}
