package sn.cse.notification.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.security.CurrentUser;
import sn.cse.notification.api.sse.AbonnementsSse;
import sn.cse.notification.api.sse.SseProperties;
import sn.cse.notification.domain.model.Destinataire;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.domain.model.Notification;

class AbonnementsSseTest {

  private final AbonnementsSse registre =
      new AbonnementsSse(
          new SseProperties(
              Duration.ofSeconds(25), Duration.ofMinutes(30), 2, Duration.ofSeconds(5)));

  @Test
  void should_push_to_connected_user_and_role_holders_only() {
    CurrentUser rm = user("RESPONSABLE_MATERIEL");
    CurrentUser daf = user("DAF");
    registre.abonner(rm);
    registre.abonner(daf);

    assertThat(registre.diffuser(notif(Destinataire.role("RESPONSABLE_MATERIEL")))).isEqualTo(1);
    assertThat(registre.diffuser(notif(Destinataire.utilisateur(daf.id())))).isEqualTo(1);
    assertThat(registre.diffuser(notif(Destinataire.role("DIRECTION_GENERALE")))).isZero();
    registre.ping();
    assertThat(registre.connexions()).isEqualTo(2);
  }

  @Test
  void should_cap_connections_per_user() {
    CurrentUser rm = user("RESPONSABLE_MATERIEL");
    registre.abonner(rm);
    registre.abonner(rm);
    registre.abonner(rm);
    assertThat(registre.connexions()).isEqualTo(2);
    assertThat(registre.diffuser(notif(Destinataire.utilisateur(rm.id())))).isEqualTo(2);
  }

  private static CurrentUser user(String role) {
    return new CurrentUser(UUID.randomUUID(), "U", "u@cse.sn", Set.of(role), Set.of(), null);
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
        java.time.Instant.EPOCH);
  }
}
