package sn.cse.suivi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sn.cse.common.exception.ConflictException;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.StatutAffectation;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.domain.model.StatutPanne;

class EtatMaterielTest {

  @ParameterizedTest
  @CsvSource({
    "DISPONIBLE,AFFECTE,true",
    "DISPONIBLE,EN_MAINTENANCE,true",
    "DISPONIBLE,REFORME,true",
    "AFFECTE,DISPONIBLE,true",
    "AFFECTE,EN_MAINTENANCE,true",
    "EN_MAINTENANCE,DISPONIBLE,true",
    "EN_MAINTENANCE,AFFECTE,true",
    "EN_MAINTENANCE,REFORME,true",
    "DISPONIBLE,DISPONIBLE,false",
    "REFORME,DISPONIBLE,false",
    "REFORME,AFFECTE,false",
    "REFORME,EN_MAINTENANCE,false"
  })
  void should_follow_explicit_transition_table(
      EtatMateriel depuis, EtatMateriel vers, boolean autorisee) {
    assertThat(depuis.peutPasserA(vers)).isEqualTo(autorisee);
  }

  @Test
  void should_make_reforme_terminal_R13() {
    assertThat(EtatMateriel.REFORME.estTerminal()).isTrue();
    assertThat(EtatMateriel.REFORME.transitionsPossibles()).isEmpty();
    assertThat(EtatMateriel.DISPONIBLE.estTerminal()).isFalse();
    assertThatThrownBy(() -> EtatMateriel.REFORME.verifierTransition(EtatMateriel.DISPONIBLE))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("REFORME → DISPONIBLE");
  }

  @Test
  void should_chain_maintenance_statuses() {
    assertThat(StatutMaintenance.PLANIFIEE.suivant()).isEqualTo(StatutMaintenance.EN_COURS);
    assertThat(StatutMaintenance.CLOTUREE.suivant()).isNull();
    assertThatCode(() -> StatutMaintenance.EN_COURS.verifierTransition(StatutMaintenance.CLOTUREE))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () -> StatutMaintenance.PLANIFIEE.verifierTransition(StatutMaintenance.CLOTUREE))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void should_chain_panne_statuses() {
    assertThatCode(() -> StatutPanne.SIGNALEE.verifierTransition(StatutPanne.PRISE_EN_CHARGE))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> StatutPanne.SIGNALEE.verifierTransition(StatutPanne.RESOLUE))
        .isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> StatutPanne.RESOLUE.verifierTransition(StatutPanne.SIGNALEE))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void should_consider_only_open_affectations_active() {
    assertThat(StatutAffectation.EN_ATTENTE_RECEPTION.estActif()).isTrue();
    assertThat(StatutAffectation.EN_COURS.estActif()).isTrue();
    assertThat(StatutAffectation.TERMINEE.estActif()).isFalse();
  }
}
