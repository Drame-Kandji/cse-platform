package sn.cse.fournisseurs.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.fournisseurs.support.Fixtures;

class LigneCatalogueTest {

  private static DonneesLigneCatalogue donnees(String ref, long prix) {
    return new DonneesLigneCatalogue(
        ref,
        "Pelle hydraulique 20 t",
        CategorieMateriel.MATERIEL_ROULANT,
        BigDecimal.valueOf(prix),
        "unité",
        30);
  }

  @Test
  void should_signaler_changement_de_prix_when_mise_a_jour() {
    Fournisseur f = Fixtures.actif();
    LigneCatalogue l = LigneCatalogue.creer(f, donnees("PH20", 95_000_000), Instant.EPOCH);
    assertThat(l.mettreAJour(f, donnees("PH20", 95_000_000), Instant.EPOCH.plusSeconds(10)))
        .isFalse();
    assertThat(l.getDateMaj()).isEqualTo(Instant.EPOCH.plusSeconds(10));
    assertThat(l.mettreAJour(f, donnees("PH20", 98_500_000), Instant.EPOCH.plusSeconds(20)))
        .isTrue();
    assertThat(l.getPrixUnitaire()).isEqualByComparingTo("98500000");
    assertThat(l.getVersion()).isZero();
  }

  @Test
  void should_refuser_mise_a_jour_d_une_autre_reference() {
    Fournisseur f = Fixtures.actif();
    LigneCatalogue l = LigneCatalogue.creer(f, donnees("PH20", 1), Instant.EPOCH);
    assertThatThrownBy(() -> l.mettreAJour(f, donnees("AUTRE", 1), Instant.EPOCH))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> l.mettreAJour(Fixtures.actif(), donnees("PH20", 1), Instant.EPOCH))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_refuser_creation_hors_specialites() {
    Fournisseur f = Fixtures.actif();
    DonneesLigneCatalogue ciment =
        new DonneesLigneCatalogue(
            "CEM", "Ciment", CategorieMateriel.CONSOMMABLE, BigDecimal.TEN, "tonne", 2);
    assertThatThrownBy(() -> LigneCatalogue.creer(f, ciment, Instant.EPOCH))
        .isInstanceOf(BusinessRuleException.class);
  }
}
