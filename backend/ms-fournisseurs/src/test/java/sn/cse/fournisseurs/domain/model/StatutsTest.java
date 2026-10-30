package sn.cse.fournisseurs.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StatutsTest {

  @ParameterizedTest(name = "{0} → {1} : {2}")
  @CsvSource({
    "EN_ATTENTE_VALIDATION, ACTIF, true",
    "EN_ATTENTE_VALIDATION, SUSPENDU, false",
    "EN_ATTENTE_VALIDATION, BLACKLISTE, false",
    "ACTIF, SUSPENDU, true",
    "ACTIF, BLACKLISTE, false",
    "ACTIF, EN_ATTENTE_VALIDATION, false",
    "SUSPENDU, ACTIF, true",
    "SUSPENDU, BLACKLISTE, true",
    "BLACKLISTE, ACTIF, false",
    "BLACKLISTE, SUSPENDU, false"
  })
  void should_respecter_table_des_transitions_fournisseur(
      StatutFournisseur depuis, StatutFournisseur vers, boolean autorisee) {
    assertThat(depuis.peutPasserA(vers)).isEqualTo(autorisee);
  }

  @ParameterizedTest(name = "{0} → {1} : {2}")
  @CsvSource({
    "OUVERT, EN_COURS, true",
    "OUVERT, RESOLU, true",
    "OUVERT, CLASSE, true",
    "EN_COURS, EN_COURS, true",
    "EN_COURS, RESOLU, true",
    "RESOLU, EN_COURS, false",
    "CLASSE, RESOLU, false"
  })
  void should_respecter_table_des_transitions_litige(
      StatutLitige depuis, StatutLitige vers, boolean ok) {
    assertThat(depuis.peutPasserA(vers)).isEqualTo(ok);
  }

  @Test
  void should_considerer_ouvert_et_en_cours_comme_ouverts() {
    assertThat(StatutLitige.OUVERT.estOuvert()).isTrue();
    assertThat(StatutLitige.EN_COURS.estOuvert()).isTrue();
    assertThat(StatutLitige.RESOLU.estOuvert()).isFalse();
    assertThat(StatutLitige.CLASSE.estOuvert()).isFalse();
  }
}
