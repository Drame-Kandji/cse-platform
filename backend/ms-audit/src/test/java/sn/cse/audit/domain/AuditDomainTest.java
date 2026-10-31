package sn.cse.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.audit.domain.exception.EntreeAuditInvalideException;
import sn.cse.audit.domain.model.AuditLog;
import sn.cse.audit.domain.model.CritereRechercheAudit;
import sn.cse.audit.domain.model.ParametresAudit;
import sn.cse.audit.domain.model.StatistiquesAudit;
import sn.cse.audit.domain.model.StatistiquesAudit.TotalParService;
import sn.cse.audit.domain.model.TraceAction;
import sn.cse.common.exception.InvalidRequestException;

class AuditDomainTest {

  private static final Instant T0 = Instant.parse("2026-10-30T09:15:00Z");

  static TraceAction trace(String utilisateurId) {
    return new TraceAction(
        UUID.randomUUID(),
        T0,
        " ms-besoins ",
        "BESOIN_VALIDE",
        "BesoinMateriel",
        "b1",
        utilisateurId,
        "Ibrahima Fall",
        Arrays.asList("RESPONSABLE_MATERIEL", null),
        "  ",
        "corr-1");
  }

  @Test
  void should_creer_entree_normalisee_when_trace_valide_R12() {
    AuditLog log = AuditLog.nouvelle(trace("u-201"), T0.plusSeconds(2));

    assertThat(log.id()).isNotNull();
    assertThat(log.service()).isEqualTo("ms-besoins");
    assertThat(log.roles()).containsExactly("RESPONSABLE_MATERIEL");
    assertThat(log.resume()).isNull();
    assertThat(log.estActionSysteme()).isFalse();
    assertThat(log.enregistreLe()).isEqualTo(T0.plusSeconds(2));
  }

  @Test
  void should_marquer_action_systeme_when_aucun_auteur() {
    AuditLog log = AuditLog.nouvelle(trace(" "), T0);
    assertThat(log.utilisateurId()).isNull();
    assertThat(log.estActionSysteme()).isTrue();
  }

  @Test
  void should_rejeter_entree_when_champ_obligatoire_absent() {
    TraceAction sansAction =
        new TraceAction(UUID.randomUUID(), T0, "ms", null, "E", "1", null, null, null, null, null);
    assertThatThrownBy(() -> AuditLog.nouvelle(sansAction, T0))
        .isInstanceOf(EntreeAuditInvalideException.class)
        .hasMessageContaining("action");

    TraceAction sansEvenement =
        new TraceAction(null, T0, "ms", "A", "E", "1", null, null, null, null, null);
    assertThatThrownBy(() -> AuditLog.nouvelle(sansEvenement, T0))
        .isInstanceOf(IllegalArgumentException.class);

    TraceAction sansDate =
        new TraceAction(UUID.randomUUID(), null, "ms", "A", "E", "1", null, null, null, null, null);
    assertThatThrownBy(() -> AuditLog.nouvelle(sansDate, T0))
        .isInstanceOf(EntreeAuditInvalideException.class);
  }

  @Test
  void should_rejeter_entree_when_champ_trop_long() {
    TraceAction longue =
        new TraceAction(
            UUID.randomUUID(), T0, "x".repeat(81), "A", "E", "1", null, null, null, null, null);
    assertThatThrownBy(() -> AuditLog.nouvelle(longue, T0))
        .isInstanceOf(EntreeAuditInvalideException.class)
        .hasMessageContaining("trop long");
  }

  @Test
  void should_convertir_periode_en_instants_when_bornes_fournies() {
    CritereRechercheAudit c =
        new CritereRechercheAudit(
            " ", "E", " A ", null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30), " q ");

    assertThat(c.utilisateurId()).isNull();
    assertThat(c.action()).isEqualTo("A");
    assertThat(c.q()).isEqualTo("q");
    assertThat(c.debut()).contains(Instant.parse("2026-10-01T00:00:00Z"));
    assertThat(c.fin()).contains(Instant.parse("2026-10-31T00:00:00Z"));
    assertThat(new CritereRechercheAudit(null, null, null, null, null, null, null).debut())
        .isEmpty();
  }

  @Test
  void should_refuser_periode_when_debut_apres_fin() {
    assertThatThrownBy(
            () ->
                new CritereRechercheAudit(
                    null,
                    null,
                    null,
                    null,
                    LocalDate.of(2026, 10, 2),
                    LocalDate.of(2026, 10, 1),
                    null))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void should_refuser_texte_libre_when_trop_long() {
    CritereRechercheAudit c =
        new CritereRechercheAudit(null, null, null, null, null, null, "abcdef");
    c.verifierTexteLibre(6);
    assertThatThrownBy(() -> c.verifierTexteLibre(5)).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void should_trier_services_par_volume_when_statistiques() {
    StatistiquesAudit s =
        new StatistiquesAudit(
            10,
            1,
            5,
            List.of(
                new TotalParService("ms-besoins", 2),
                new TotalParService("ms-achats", 5),
                new TotalParService("ms-auth", 2)));
    assertThat(s.parService())
        .extracting(TotalParService::service)
        .containsExactly("ms-achats", "ms-auth", "ms-besoins");
    assertThat(new StatistiquesAudit(0, 0, 0, null).parService()).isEmpty();
  }

  @Test
  void should_valider_parametres() {
    assertThat(new ParametresAudit(7, 100).fenetreStatistiquesJours()).isEqualTo(7);
    assertThatThrownBy(() -> new ParametresAudit(0, 100))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ParametresAudit(7, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
