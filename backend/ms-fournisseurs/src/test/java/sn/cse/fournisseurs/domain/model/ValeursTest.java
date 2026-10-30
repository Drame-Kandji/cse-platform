package sn.cse.fournisseurs.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.fournisseurs.support.Fixtures;

class ValeursTest {

  @Test
  void should_normaliser_le_profil() {
    ProfilFournisseur p =
        new ProfilFournisseur(
            " Sahel ",
            "005123456 2g3",
            null,
            null,
            null,
            "Sénégal",
            null,
            " A@B.SN ",
            null,
            null,
            Set.of(CategorieMateriel.OUTILLAGE),
            Arrays.asList("ISO 9001", " ISO 9001 ", "", null));
    assertThat(p.raisonSociale()).isEqualTo("Sahel");
    assertThat(p.ninea()).isEqualTo("005123456 2G3");
    assertThat(p.email()).isEqualTo("a@b.sn");
    assertThat(p.certifications()).containsExactly("ISO 9001");
    assertThat(p.couvre(CategorieMateriel.OUTILLAGE)).isTrue();
    assertThat(p.couvre(CategorieMateriel.CONSOMMABLE)).isFalse();
    assertThat(
            new ProfilFournisseur(
                    "S",
                    "1",
                    null,
                    null,
                    null,
                    "SN",
                    null,
                    "e@e.sn",
                    null,
                    null,
                    Set.of(CategorieMateriel.OUTILLAGE),
                    null)
                .certifications())
        .isEmpty();
  }

  @Test
  void should_refuser_profil_incomplet() {
    assertThatThrownBy(
            () ->
                new ProfilFournisseur(
                    "S", "1", null, null, null, "SN", null, "e@e.sn", null, null, Set.of(),
                    List.of()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("spécialité");
    assertThatThrownBy(
            () ->
                new ProfilFournisseur(
                    " ",
                    "1",
                    null,
                    null,
                    null,
                    "SN",
                    null,
                    "e@e.sn",
                    null,
                    null,
                    EnumSet.of(CategorieMateriel.OUTILLAGE),
                    List.of()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("raison sociale");
  }

  @Test
  void should_normaliser_iban_et_exiger_banque() {
    assertThat(new CoordonneesBancaires(" BSC ", "sn08 sn01 0015").iban())
        .isEqualTo("SN08SN010015");
    assertThatThrownBy(() -> new CoordonneesBancaires("", "SN08"))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> new CoordonneesBancaires("BSC", null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_borner_les_notes_entre_0_et_10_R07() {
    assertThat(new NotesEvaluation(0, 10, 5, 5, 5).delais()).isEqualTo(10);
    assertThatThrownBy(() -> new NotesEvaluation(11, 5, 5, 5, 5))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("qualite");
    assertThatThrownBy(() -> new NotesEvaluation(5, 5, 5, 5, -1))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("prix");
  }

  @Test
  void should_valider_les_compteurs_de_performance() {
    assertThat(new Performance(null, 0, 0, 0).scorePerformance()).isEqualByComparingTo("0");
    assertThatThrownBy(() -> new Performance(BigDecimal.ONE, -1, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Performance(BigDecimal.ONE, 0, 1, 2))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_valider_et_normaliser_ligne_catalogue() {
    DonneesLigneCatalogue d =
        new DonneesLigneCatalogue(
            " ph-20 ",
            "Pelle",
            CategorieMateriel.MATERIEL_ROULANT,
            new BigDecimal("1000.4"),
            "unité",
            3);
    assertThat(d.reference()).isEqualTo("PH-20");
    assertThat(d.prixUnitaire()).isEqualByComparingTo("1000");
    assertThatThrownBy(
            () ->
                new DonneesLigneCatalogue(
                    "R", "D", CategorieMateriel.OUTILLAGE, BigDecimal.ZERO, "u", 1))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                new DonneesLigneCatalogue(
                    "R", "D", CategorieMateriel.OUTILLAGE, BigDecimal.ONE, "u", -1))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                new DonneesLigneCatalogue(
                    "R", " ", CategorieMateriel.OUTILLAGE, BigDecimal.ONE, "u", 1))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_exiger_libelle_document() {
    assertThatThrownBy(() -> DocumentAdministratif.creer(TypeDocument.AUTRE, " ", null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_normaliser_commentaire_evaluation() {
    Fournisseur f = Fixtures.actif();
    NotesEvaluation n = new NotesEvaluation(5, 5, 5, 5, 5);
    Evaluation e = Evaluation.creer(f, null, n, BigDecimal.TEN, "  ", Fixtures.RA, Instant.EPOCH);
    assertThat(e.commentaire()).isNull();
    assertThat(e.fournisseurId()).isEqualTo(f.getId());
    assertThat(Auteur.SYSTEME.id()).isNull();
  }

  @Test
  void should_historiser_le_prix_courant_d_une_ligne() {
    Fournisseur f = Fixtures.actif();
    LigneCatalogue l =
        LigneCatalogue.creer(
            f,
            new DonneesLigneCatalogue(
                "R1", "Pelle", CategorieMateriel.MATERIEL_ROULANT, BigDecimal.TEN, "unité", 2),
            Instant.EPOCH);
    PrixHistorique h = PrixHistorique.depuis(l);
    assertThat(h.reference()).isEqualTo("R1");
    assertThat(h.prixUnitaire()).isEqualByComparingTo("10");
    assertThat(h.date()).isEqualTo(Instant.EPOCH);
    assertThat(h.fournisseurId()).isEqualTo(f.getId());
  }
}
