package sn.cse.fournisseurs.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.DonneesLigneCatalogue;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.LigneCatalogue;
import sn.cse.fournisseurs.support.Fixtures;

class ComparateurPrixTest {

  private static ComparateurPrix.OffreCatalogue offre(
      Fournisseur f, String ref, String designation, long prix) {
    LigneCatalogue l =
        LigneCatalogue.creer(
            f,
            new DonneesLigneCatalogue(
                ref,
                designation,
                CategorieMateriel.OUTILLAGE,
                BigDecimal.valueOf(prix),
                "unité",
                10),
            Instant.EPOCH);
    return new ComparateurPrix.OffreCatalogue(
        l, f.getProfil().raisonSociale(), BigDecimal.valueOf(70));
  }

  @Test
  void should_regrouper_par_designation_normalisee_et_calculer_les_prix() {
    Fournisseur a =
        Fixtures.actif("Dakar Outillage Pro", "004345678 1A2", CategorieMateriel.OUTILLAGE);
    Fournisseur b =
        Fixtures.actif("Teranga Équipements BTP", "006234567 2V1", CategorieMateriel.OUTILLAGE);
    List<ComparateurPrix.ComparaisonPrix> resultat =
        ComparateurPrix.comparer(
            List.of(
                offre(b, "TEB-GE100", "Groupe électrogène 100 kVA", 15_900_000),
                offre(a, "DOP-GE100", "groupe  ELECTROGENE 100 kva", 14_200_000),
                offre(a, "DOP-PLQ", "Plaque vibrante", 1_850_000)));
    assertThat(resultat).hasSize(2);
    ComparateurPrix.ComparaisonPrix groupe = resultat.get(0);
    assertThat(groupe.designation()).isEqualTo("groupe  ELECTROGENE 100 kva");
    assertThat(groupe.reference()).isNull();
    assertThat(groupe.prixMin()).isEqualByComparingTo("14200000");
    assertThat(groupe.prixMax()).isEqualByComparingTo("15900000");
    assertThat(groupe.prixMoyen()).isEqualByComparingTo("15050000");
    assertThat(groupe.offres())
        .extracting(ComparateurPrix.OffrePrix::raisonSociale)
        .containsExactly("Dakar Outillage Pro", "Teranga Équipements BTP");
    ComparateurPrix.ComparaisonPrix plaque = resultat.get(1);
    assertThat(plaque.reference()).isEqualTo("DOP-PLQ");
    assertThat(plaque.offres())
        .singleElement()
        .extracting(ComparateurPrix.OffrePrix::delaiLivraisonJours)
        .isEqualTo(10);
  }

  @Test
  void should_renvoyer_liste_vide_sans_offre() {
    assertThat(ComparateurPrix.comparer(List.of())).isEmpty();
    assertThat(ComparateurPrix.normaliser("  Électrogène   ÀB ")).isEqualTo("electrogene ab");
  }
}
