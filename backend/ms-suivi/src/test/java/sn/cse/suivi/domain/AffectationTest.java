package sn.cse.suivi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Periode;
import sn.cse.suivi.domain.model.StatutAffectation;
import sn.cse.suivi.domain.service.PolitiqueAffectation;

class AffectationTest {

  private static final LocalDate J = Fixtures.AUJOURDHUI;
  private static final Instant MAINTENANT = Instant.parse("2026-10-30T09:00:00Z");

  private static Affectation affectation(LocalDate debut, LocalDate fin) {
    return PolitiqueAffectation.affecter(
        Fixtures.materiel(), Fixtures.chantier(), null, new Periode(debut, fin), List.of());
  }

  @Test
  void should_create_waiting_reception_and_affect_materiel_R04() {
    Materiel materiel = Fixtures.materiel();
    Chantier chantier = Fixtures.chantier();
    UUID besoin = UUID.randomUUID();

    Affectation a =
        PolitiqueAffectation.affecter(
            materiel, chantier, besoin, new Periode(J, J.plusDays(30)), List.of());

    assertThat(a.getStatut()).isEqualTo(StatutAffectation.EN_ATTENTE_RECEPTION);
    assertThat(a.getBesoinId()).isEqualTo(besoin);
    assertThat(a.getMaterielId()).isEqualTo(materiel.getId());
    assertThat(a.getChantierId()).isEqualTo(chantier.getId());
    assertThat(a.estActive()).isTrue();
    assertThat(materiel.getEtat()).isEqualTo(EtatMateriel.AFFECTE);
  }

  @Test
  void should_detect_overlap_with_active_affectation_as_conflict() {
    Materiel materiel = Fixtures.materiel();
    Affectation existante = affectation(J, J.plusDays(30));

    assertThatThrownBy(
            () ->
                PolitiqueAffectation.affecter(
                    materiel,
                    Fixtures.chantier(),
                    null,
                    new Periode(J.plusDays(30), J.plusDays(60)),
                    List.of(existante)))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo("CONFLIT_AFFECTATION");
    assertThat(materiel.getEtat()).isEqualTo(EtatMateriel.DISPONIBLE);
  }

  @Test
  void should_ignore_terminated_or_non_overlapping_affectations() {
    Affectation terminee = affectation(J.minusDays(60), J.minusDays(10));
    terminee.terminer(J.minusDays(10), null);
    Affectation future = affectation(J.plusDays(100), J.plusDays(130));

    Affectation a =
        PolitiqueAffectation.affecter(
            Fixtures.materiel(),
            Fixtures.chantier(),
            null,
            new Periode(J, J.plusDays(30)),
            List.of(terminee, future));

    assertThat(a.estActive()).isTrue();
  }

  @Test
  void should_refuse_affectation_to_terminated_site() {
    Chantier chantier = Fixtures.chantier();
    chantier.terminer(0);

    assertThatThrownBy(
            () ->
                PolitiqueAffectation.affecter(
                    Fixtures.materiel(), chantier, null, new Periode(J, J), List.of()))
        .extracting("code")
        .isEqualTo("CHANTIER_TERMINE");
  }

  @Test
  void should_refuse_materiel_in_maintenance_before_checking_overlaps_R04() {
    Materiel materiel = Fixtures.materiel();
    materiel.mettreEnMaintenance();

    assertThatThrownBy(
            () ->
                PolitiqueAffectation.affecter(
                    materiel, Fixtures.chantier(), null, new Periode(J, J), List.of()))
        .isInstanceOf(BusinessRuleException.class)
        .extracting("code")
        .isEqualTo("MATERIEL_NON_DISPONIBLE");
  }

  @Test
  void should_confirm_reception_once() {
    Affectation a = affectation(J, J.plusDays(30));

    a.confirmerReception(MAINTENANT, "RAS");

    assertThat(a.getStatut()).isEqualTo(StatutAffectation.EN_COURS);
    assertThat(a.getDateReception()).isEqualTo(MAINTENANT);
    assertThat(a.getObservationsReception()).isEqualTo("RAS");
    assertThatThrownBy(() -> a.confirmerReception(MAINTENANT, null))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void should_terminate_with_real_end_date() {
    Affectation a = affectation(J.minusDays(20), J.plusDays(30));

    assertThatThrownBy(() -> a.terminer(J.minusDays(21), null))
        .isInstanceOf(BusinessRuleException.class);
    a.terminer(J, "Fin anticipée");

    assertThat(a.getStatut()).isEqualTo(StatutAffectation.TERMINEE);
    assertThat(a.getDateFinReelle()).isEqualTo(J);
    assertThat(a.periodeEffective()).isEqualTo(new Periode(J.minusDays(20), J));
    assertThat(a.bloque(new Periode(J, J.plusDays(1)))).isFalse();
    assertThatThrownBy(() -> a.terminer(J, null)).isInstanceOf(ConflictException.class);
  }

  @Test
  void should_alert_end_of_affectation_once_within_window() {
    Affectation a = affectation(J.minusDays(20), J.plusDays(3));

    assertThat(a.periodeEffective().fin()).isEqualTo(J.plusDays(3));
    assertThat(a.doitAlerterFinProchaine(J.minusDays(1), 3)).isFalse();
    assertThat(a.doitAlerterFinProchaine(J, 3)).isTrue();
    assertThat(a.doitAlerterFinProchaine(J.plusDays(4), 3)).isFalse();
    a.marquerAlerteFinEnvoyee();
    assertThat(a.doitAlerterFinProchaine(J, 3)).isFalse();
  }

  @Test
  void should_round_trip_through_snapshot() {
    Affectation a = affectation(J, J.plusDays(5));

    Affectation copie = Affectation.reconstituer(a.snapshot());

    assertThat(copie.snapshot()).isEqualTo(a.snapshot());
    assertThat(copie.getDateDebut()).isEqualTo(J);
    assertThat(copie.getDateFinPrevue()).isEqualTo(J.plusDays(5));
  }
}
