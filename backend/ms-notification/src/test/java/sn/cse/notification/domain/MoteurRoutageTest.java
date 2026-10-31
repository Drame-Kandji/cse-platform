package sn.cse.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static sn.cse.notification.domain.Fixtures.evenement;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.notification.domain.model.ConditionRegle;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.domain.model.NotificationPlanifiee;
import sn.cse.notification.domain.model.Operateur;
import sn.cse.notification.domain.service.FormateurValeurs;
import sn.cse.notification.domain.service.MoteurRoutage;

class MoteurRoutageTest {

  private final FormateurValeurs formateur =
      new FormateurValeurs(Map.of("BLOQUANTE", "bloquante", "URGENT", "urgente"));

  @Test
  void should_route_to_role_with_resolved_texts_when_rule_matches() {
    MoteurRoutage moteur =
        new MoteurRoutage(
            List.of(
                Fixtures.regleRole(
                    "BESOIN_SOUMIS", "RESPONSABLE_MATERIEL", "Besoin {reference} ({priorite})")),
            formateur,
            "—");
    UUID besoinId = UUID.randomUUID();
    List<NotificationPlanifiee> plans =
        moteur.router(
            evenement(
                "BESOIN_SOUMIS",
                Map.of(
                    "reference",
                    "BES-2026-0012",
                    "besoinId",
                    besoinId.toString(),
                    "priorite",
                    "URGENT")));

    assertThat(plans).hasSize(1);
    NotificationPlanifiee p = plans.get(0);
    assertThat(p.titre()).isEqualTo("Besoin BES-2026-0012 (urgente)");
    assertThat(p.message()).isEqualTo("Message BES-2026-0012");
    assertThat(p.lien()).isEqualTo("/besoins/" + besoinId);
    assertThat(p.entiteId()).isEqualTo(besoinId.toString());
    assertThat(p.roles()).containsExactly("RESPONSABLE_MATERIEL");
    assertThat(p.utilisateurs()).isEmpty();
  }

  @Test
  void should_ignore_event_when_no_rule_matches() {
    MoteurRoutage moteur =
        new MoteurRoutage(List.of(Fixtures.regleRole("BESOIN_SOUMIS", "DAF", "t")), formateur, "—");
    assertThat(moteur.router(evenement("BESOIN_MODIFIE", Map.of()))).isEmpty();
    assertThat(moteur.estRoute("BESOIN_SOUMIS")).isTrue();
    assertThat(moteur.estRoute("AUDIT")).isFalse();
  }

  @Test
  void should_route_to_payload_user_when_field_holds_uuid() {
    UUID demandeur = UUID.randomUUID();
    MoteurRoutage moteur =
        new MoteurRoutage(
            List.of(Fixtures.regleUtilisateur("BESOIN_VALIDE", "demandeurId")), formateur, "—");
    List<NotificationPlanifiee> plans =
        moteur.router(
            evenement(
                "BESOIN_VALIDE",
                Map.of("demandeurId", demandeur.toString(), "reference", "BES-1")));
    assertThat(plans)
        .singleElement()
        .satisfies(
            p -> {
              assertThat(p.utilisateurs()).containsExactly(demandeur);
              assertThat(p.message()).isEqualTo("Validé par Ibrahima Fall");
              assertThat(p.email()).isTrue();
              // besoinId absent : le lien est abandonné plutôt que de produire une route invalide.
              assertThat(p.lien()).isNull();
            });
  }

  @Test
  void should_skip_rule_when_user_field_missing_or_invalid() {
    MoteurRoutage moteur =
        new MoteurRoutage(
            List.of(Fixtures.regleUtilisateur("BESOIN_VALIDE", "demandeurId")), formateur, "—");
    assertThat(moteur.router(evenement("BESOIN_VALIDE", Map.of()))).isEmpty();
    assertThat(moteur.router(evenement("BESOIN_VALIDE", Map.of("demandeurId", "pas-un-uuid"))))
        .isEmpty();
  }

  @Test
  void should_request_chefs_of_payload_chantier_when_rule_targets_chefs() {
    UUID chantier = UUID.randomUUID();
    MoteurRoutage moteur =
        new MoteurRoutage(List.of(Fixtures.regleChefs("MATERIEL_AFFECTE")), formateur, "—");
    NotificationPlanifiee p =
        moteur
            .router(
                evenement(
                    "MATERIEL_AFFECTE",
                    Map.of(
                        "chantierId",
                        chantier.toString(),
                        "materielCode",
                        "PEL-001",
                        "chantierNom",
                        "Autoroute")))
            .get(0);
    assertThat(p.chantierPourChefs()).isEqualTo(chantier);
    assertThat(p.titre()).isEqualTo("Matériel PEL-001 affecté");
    assertThat(p.message()).isEqualTo("Au chantier Autoroute");
  }

  @Test
  void should_use_fallback_text_when_placeholder_missing() {
    MoteurRoutage moteur =
        new MoteurRoutage(
            List.of(Fixtures.regleRole("X", "DAF", "Contrat {numero}")), formateur, "—");
    assertThat(moteur.router(evenement("X", Map.of())).get(0).titre()).isEqualTo("Contrat —");
  }

  @Test
  void should_pick_level_by_condition_when_panne_bloquante() {
    MoteurRoutage moteur =
        new MoteurRoutage(
            List.of(
                Fixtures.regleConditionnelle(
                    "PANNE_SIGNALEE",
                    "RESPONSABLE_MATERIEL",
                    new ConditionRegle("gravite", Operateur.EGAL, "BLOQUANTE"),
                    NiveauNotification.CRITIQUE),
                Fixtures.regleConditionnelle(
                    "PANNE_SIGNALEE",
                    "RESPONSABLE_MATERIEL",
                    new ConditionRegle("gravite", Operateur.DIFFERENT, "BLOQUANTE"),
                    NiveauNotification.ALERTE)),
            formateur,
            "—");
    assertThat(moteur.router(evenement("PANNE_SIGNALEE", Map.of("gravite", "BLOQUANTE"))))
        .extracting(NotificationPlanifiee::niveau)
        .containsExactly(NiveauNotification.CRITIQUE);
    assertThat(moteur.router(evenement("PANNE_SIGNALEE", Map.of("gravite", "MINEURE"))))
        .extracting(NotificationPlanifiee::niveau)
        .containsExactly(NiveauNotification.ALERTE);
  }
}
