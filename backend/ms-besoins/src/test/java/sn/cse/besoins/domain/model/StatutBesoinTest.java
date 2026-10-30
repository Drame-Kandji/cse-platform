package sn.cse.besoins.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatutBesoinTest {

  @Test
  void should_autoriser_uniquement_les_transitions_de_la_machine_a_etats() {
    assertThat(StatutBesoin.EN_ATTENTE.suivantsPossibles())
        .containsExactlyInAnyOrder(StatutBesoin.VALIDE, StatutBesoin.REJETE);
    assertThat(StatutBesoin.VALIDE.suivantsPossibles())
        .containsExactlyInAnyOrder(StatutBesoin.AFFECTE_DIRECT, StatutBesoin.EN_ACHAT);
    assertThat(StatutBesoin.EN_ATTENTE.peutPasserA(StatutBesoin.AFFECTE_DIRECT)).isFalse();
    assertThat(StatutBesoin.VALIDE.peutPasserA(StatutBesoin.REJETE)).isFalse();
  }

  @Test
  void should_considerer_rejete_affecte_et_achat_comme_terminaux() {
    assertThat(StatutBesoin.REJETE.estTerminal()).isTrue();
    assertThat(StatutBesoin.AFFECTE_DIRECT.estTerminal()).isTrue();
    assertThat(StatutBesoin.EN_ACHAT.estTerminal()).isTrue();
    assertThat(StatutBesoin.VALIDE.estTerminal()).isFalse();
  }

  @Test
  void should_n_etre_modifiable_qu_en_attente_R05() {
    assertThat(StatutBesoin.EN_ATTENTE.estModifiable()).isTrue();
    for (StatutBesoin s :
        new StatutBesoin[] {
          StatutBesoin.VALIDE,
          StatutBesoin.REJETE,
          StatutBesoin.AFFECTE_DIRECT,
          StatutBesoin.EN_ACHAT
        }) {
      assertThat(s.estModifiable()).as(s.name()).isFalse();
    }
  }

  @Test
  void should_exiger_justification_pour_urgent_et_critique_R08() {
    assertThat(Priorite.NORMAL.exigeJustification()).isFalse();
    assertThat(Priorite.URGENT.exigeJustification()).isTrue();
    assertThat(Priorite.CRITIQUE.exigeJustification()).isTrue();
    assertThat(Priorite.NORMAL.max(Priorite.CRITIQUE)).isEqualTo(Priorite.CRITIQUE);
    assertThat(Priorite.URGENT.max(null)).isEqualTo(Priorite.URGENT);
  }
}
