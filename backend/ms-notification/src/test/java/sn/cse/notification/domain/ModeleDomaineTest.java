package sn.cse.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.notification.domain.model.Destinataire;
import sn.cse.notification.domain.model.EvenementMetier;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.domain.model.Notification;
import sn.cse.notification.domain.model.NotificationPlanifiee;
import sn.cse.notification.domain.model.RegleRoutage;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;

class ModeleDomaineTest {

  @Test
  void should_create_one_notification_per_role_and_per_distinct_user() {
    UUID u1 = UUID.randomUUID();
    UUID chef = UUID.randomUUID();
    NotificationPlanifiee plan =
        new NotificationPlanifiee(
            "T",
            "t",
            "m",
            NiveauNotification.INFO,
            null,
            null,
            null,
            Set.of("DAF", "RESPONSABLE_ACHATS"),
            Set.of(u1),
            null,
            false);
    List<Notification> notifs =
        plan.materialiser(List.of(u1, chef), UUID.randomUUID(), Fixtures.T0);
    assertThat(notifs).hasSize(4);
    assertThat(notifs)
        .extracting(Notification::destinataire)
        .containsExactly(
            Destinataire.role("DAF"),
            Destinataire.role("RESPONSABLE_ACHATS"),
            Destinataire.utilisateur(u1),
            Destinataire.utilisateur(chef));
  }

  @Test
  void should_tell_whether_notification_targets_user_or_role() {
    UUID moi = UUID.randomUUID();
    Notification perso = notif(Destinataire.utilisateur(moi));
    Notification role = notif(Destinataire.role("DAF"));
    assertThat(perso.estDestineeA(moi, Set.of())).isTrue();
    assertThat(perso.estDestineeA(UUID.randomUUID(), Set.of("DAF"))).isFalse();
    assertThat(role.estDestineeA(UUID.randomUUID(), Set.of("DAF"))).isTrue();
    assertThat(role.estDestineeA(moi, Set.of("RESPONSABLE_ACHATS"))).isFalse();
  }

  @Test
  void should_reject_invalid_recipients_and_rules() {
    assertThatThrownBy(() -> Destinataire.role(" ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Destinataire.utilisateur(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                new RegleRoutage(
                    "X",
                    Set.of(),
                    List.of(),
                    false,
                    null,
                    NiveauNotification.INFO,
                    "t",
                    "m",
                    null,
                    null,
                    null,
                    false,
                    null))
        .hasMessageContaining("aucun destinataire");
    assertThatThrownBy(
            () ->
                new RegleRoutage(
                    "X",
                    null,
                    null,
                    true,
                    " ",
                    NiveauNotification.INFO,
                    "t",
                    "m",
                    null,
                    null,
                    null,
                    false,
                    null))
        .hasMessageContaining("champ chantier");
  }

  @Test
  void should_keep_newest_directory_version_and_compute_display_name() {
    UUID id = UUID.randomUUID();
    UtilisateurAnnuaire v1 =
        new UtilisateurAnnuaire(
            id, "a@cse.sn", "Awa", "Ndiaye", "CHEF_CHANTIER", null, true, Fixtures.T0);
    UtilisateurAnnuaire ancien =
        new UtilisateurAnnuaire(
            id,
            "a@cse.sn",
            null,
            null,
            "CHEF_CHANTIER",
            Set.of(),
            false,
            Fixtures.T0.minusSeconds(60));
    assertThat(v1.estRemplaceePar(ancien)).isFalse();
    assertThat(ancien.estRemplaceePar(v1)).isTrue();
    assertThat(v1.nomComplet()).isEqualTo("Awa Ndiaye");
    assertThat(ancien.nomComplet()).isEqualTo("a@cse.sn");
    assertThat(v1.joignableParEmail()).isTrue();
    assertThat(ancien.joignableParEmail()).isFalse();
    assertThat(
            new UtilisateurAnnuaire(id, " ", "A", null, "DAF", Set.of(), true, Fixtures.T0)
                .joignableParEmail())
        .isFalse();
  }

  @Test
  void should_expose_virtual_fields_of_event() {
    EvenementMetier e =
        new EvenementMetier(UUID.randomUUID(), "T", "agg", Instant.EPOCH, "Moi", null);
    assertThat(e.valeur("acteurNom")).contains("Moi");
    assertThat(e.valeur("aggregateId")).contains("agg");
    assertThat(e.valeur("x")).isEmpty();
    assertThat(Fixtures.evenement("T", Map.of("id", "x")).uuid("id")).isEmpty();
  }

  private static Notification notif(Destinataire d) {
    return new Notification(
        UUID.randomUUID(),
        "T",
        "t",
        "m",
        NiveauNotification.INFO,
        null,
        null,
        null,
        d,
        UUID.randomUUID(),
        Fixtures.T0);
  }
}
