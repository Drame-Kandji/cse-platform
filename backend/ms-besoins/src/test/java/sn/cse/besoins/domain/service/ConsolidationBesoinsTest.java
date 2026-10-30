package sn.cse.besoins.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;

class ConsolidationBesoinsTest {

  private static final UUID CH1 = UUID.randomUUID();
  private static final UUID CH2 = UUID.randomUUID();
  private static final LocalDate J = LocalDate.parse("2026-11-01");

  private static BesoinAConsolider b(
      UUID chantier, String designation, CategorieMateriel cat, int qte, Priorite p, LocalDate d) {
    return new BesoinAConsolider(
        UUID.randomUUID(), chantier, "Chantier " + chantier, cat, designation, qte, p, d);
  }

  @Test
  void should_regrouper_par_categorie_et_designation_normalisee() {
    List<LigneConsolidation> lignes =
        new ConsolidationBesoins(30)
            .consolider(
                List.of(
                    b(
                        CH1,
                        "Pelle hydraulique 20 t",
                        CategorieMateriel.MATERIEL_ROULANT,
                        1,
                        Priorite.NORMAL,
                        J),
                    b(
                        CH2,
                        "pelle  HYDRAULIQUE 20T",
                        CategorieMateriel.MATERIEL_ROULANT,
                        2,
                        Priorite.CRITIQUE,
                        J),
                    b(
                        CH2,
                        "Pelle hydraulique 20 t",
                        CategorieMateriel.MATERIEL_ROULANT,
                        1,
                        Priorite.URGENT,
                        J.plusDays(90)),
                    b(
                        CH1,
                        "Pelle hydraulique 20 t",
                        CategorieMateriel.OUTILLAGE,
                        4,
                        Priorite.NORMAL,
                        J),
                    b(
                        CH1,
                        "Bétonnière 350 L",
                        CategorieMateriel.OUTILLAGE,
                        1,
                        Priorite.NORMAL,
                        J)));

    assertThat(lignes).hasSize(3);
    LigneConsolidation pelles = lignes.get(0);
    assertThat(pelles.categorie()).isEqualTo(CategorieMateriel.MATERIEL_ROULANT);
    assertThat(pelles.designation()).isEqualTo("Pelle hydraulique 20 t");
    assertThat(pelles.quantiteTotale()).isEqualTo(4);
    assertThat(pelles.nombreDemandes()).isEqualTo(3);
    assertThat(pelles.prioriteMax()).isEqualTo(Priorite.CRITIQUE);
    assertThat(pelles.chantiers())
        .extracting(LigneConsolidation.ChantierConcerne::id)
        .containsExactly(CH1, CH2);
    assertThat(pelles.besoinIds()).hasSize(3);
    // CH2 a deux demandes, mais distantes de 90 jours > fenêtre de 30 jours.
    assertThat(pelles.doublonPotentiel()).isFalse();
    // Tri : priorité décroissante puis quantité décroissante.
    assertThat(lignes.get(1).quantiteTotale()).isEqualTo(4);
  }

  @Test
  void should_signaler_un_doublon_potentiel_dans_la_fenetre() {
    List<LigneConsolidation> lignes =
        new ConsolidationBesoins(30)
            .consolider(
                List.of(
                    b(CH1, "Bétonnière 350 L", CategorieMateriel.OUTILLAGE, 2, Priorite.NORMAL, J),
                    b(
                        CH1,
                        "betonniere 350 l",
                        CategorieMateriel.OUTILLAGE,
                        1,
                        Priorite.URGENT,
                        J.plusDays(30))));

    assertThat(lignes)
        .singleElement()
        .satisfies(
            l -> {
              assertThat(l.doublonPotentiel()).isTrue();
              assertThat(l.quantiteTotale()).isEqualTo(3);
            });
  }

  @Test
  void should_normaliser_les_designations() {
    assertThat(NormalisationDesignation.normaliser("  Bétonnière   350 L "))
        .isEqualTo("betonniere 350l");
    assertThat(NormalisationDesignation.normaliser("Pelle-hydraulique (20 T)"))
        .isEqualTo("pelle hydraulique 20t");
    assertThat(NormalisationDesignation.normaliser(null)).isEmpty();
  }

  @Test
  void should_refuser_une_fenetre_negative() {
    assertThatThrownBy(() -> new ConsolidationBesoins(-1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new ConsolidationBesoins(0).consolider(List.of())).isEmpty();
  }

  @Test
  void should_ordonner_les_materiels_souhaites_en_premier() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    UUID c = UUID.randomUUID();
    UUID inconnu = UUID.randomUUID();

    assertThat(SelectionMateriels.candidats(List.of(a, b, c), List.of(c, inconnu), List.of(b)))
        .containsExactly(c, a);
    assertThat(SelectionMateriels.candidats(null, null, null)).isEmpty();
  }
}
