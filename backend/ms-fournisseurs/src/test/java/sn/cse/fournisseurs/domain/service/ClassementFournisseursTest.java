package sn.cse.fournisseurs.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.support.Fixtures;

class ClassementFournisseursTest {

  private static Fournisseur avecScore(
      String nom, String ninea, String score, CategorieMateriel cat) {
    Fournisseur f = Fixtures.actif(nom, ninea, cat);
    f.actualiserScore(new BigDecimal(score), 3);
    return f;
  }

  @Test
  void should_classer_uniquement_les_actifs_par_score_decroissant() {
    Fournisseur a = avecScore("Alpha", "001", "70.0", CategorieMateriel.OUTILLAGE);
    Fournisseur b = avecScore("Beta", "002", "85.0", CategorieMateriel.OUTILLAGE);
    Fournisseur c = avecScore("Gamma", "003", "70.0", CategorieMateriel.MATERIEL_ROULANT);
    c.enregistrerLivraison(LocalDate.EPOCH, LocalDate.EPOCH);
    Fournisseur suspendu = avecScore("Delta", "004", "99.0", CategorieMateriel.OUTILLAGE);
    suspendu.suspendre("motif");
    List<ClassementFournisseurs.Rang> rangs =
        ClassementFournisseurs.classer(
            List.of(
                new ClassementFournisseurs.Candidat(a, 0),
                new ClassementFournisseurs.Candidat(b, 2),
                new ClassementFournisseurs.Candidat(c, 1),
                new ClassementFournisseurs.Candidat(suspendu, 0)),
            null);
    assertThat(rangs)
        .extracting(r -> r.fournisseur().getProfil().raisonSociale())
        .containsExactly("Beta", "Gamma", "Alpha");
    assertThat(rangs).extracting(ClassementFournisseurs.Rang::rang).containsExactly(1, 2, 3);
    assertThat(rangs.get(0).nombreLitiges()).isEqualTo(2);
  }

  @Test
  void should_filtrer_par_categorie() {
    Fournisseur a = avecScore("Alpha", "001", "70.0", CategorieMateriel.OUTILLAGE);
    Fournisseur c = avecScore("Gamma", "003", "90.0", CategorieMateriel.MATERIEL_ROULANT);
    assertThat(
            ClassementFournisseurs.classer(
                List.of(
                    new ClassementFournisseurs.Candidat(a, 0),
                    new ClassementFournisseurs.Candidat(c, 0)),
                CategorieMateriel.OUTILLAGE))
        .singleElement()
        .extracting(r -> r.fournisseur().getId())
        .isEqualTo(a.getId());
  }
}
