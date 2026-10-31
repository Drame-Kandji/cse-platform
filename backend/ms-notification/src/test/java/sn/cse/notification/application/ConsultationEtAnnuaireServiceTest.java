package sn.cse.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.security.CurrentUser;
import sn.cse.notification.application.service.AnnuaireService;
import sn.cse.notification.application.service.ConsultationNotificationsService;
import sn.cse.notification.domain.Fixtures;
import sn.cse.notification.domain.model.Destinataire;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.domain.model.Notification;
import sn.cse.notification.domain.model.NotificationVue;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;

class ConsultationEtAnnuaireServiceTest {

  private final FakeRepositories.Notifications repo = new FakeRepositories.Notifications();
  private final ConsultationNotificationsService service =
      new ConsultationNotificationsService(repo, Clock.fixed(Fixtures.T0, ZoneOffset.UTC));
  private final PageQuery page = new PageQuery(0, 20, "createdAt", false);

  private final CurrentUser rm1 = user("RESPONSABLE_MATERIEL");
  private final CurrentUser rm2 = user("RESPONSABLE_MATERIEL");
  private final CurrentUser daf = user("DAF");

  @Test
  void should_track_read_state_per_user_for_role_notifications() {
    Notification pourRm = notif(Destinataire.role("RESPONSABLE_MATERIEL"), 0);
    Notification perso = notif(Destinataire.utilisateur(rm1.id()), 1);
    repo.enregistrer(List.of(pourRm, perso, notif(Destinataire.role("DAF"), 2)));

    assertThat(service.compterNonLues(rm1)).isEqualTo(2);
    assertThat(service.compterNonLues(rm2)).isEqualTo(1);

    NotificationVue lue = service.marquerLue(rm1, pourRm.id());
    assertThat(lue.lue()).isTrue();
    assertThat(service.compterNonLues(rm1)).isEqualTo(1);
    // La lecture par rm1 n'affecte pas rm2.
    assertThat(service.compterNonLues(rm2)).isEqualTo(1);

    assertThat(service.lister(rm1, null, page).content())
        .extracting(v -> v.notification().id(), NotificationVue::lue)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(perso.id(), false),
            org.assertj.core.groups.Tuple.tuple(pourRm.id(), true));
    assertThat(service.lister(rm1, true, page).content())
        .extracting(NotificationVue::lue)
        .containsExactly(true);
    assertThat(service.lister(rm1, false, page).content()).hasSize(1);
  }

  @Test
  void should_return_404_when_marking_notification_of_someone_else() {
    Notification pourDaf = notif(Destinataire.role("DAF"), 0);
    repo.enregistrer(List.of(pourDaf));
    assertThatThrownBy(() -> service.marquerLue(rm1, pourDaf.id()))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.marquerLue(rm1, UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(service.marquerLue(daf, pourDaf.id()).lue()).isTrue();
  }

  @Test
  void should_mark_all_visible_as_read() {
    repo.enregistrer(
        List.of(
            notif(Destinataire.role("RESPONSABLE_MATERIEL"), 0),
            notif(Destinataire.utilisateur(rm1.id()), 1),
            notif(Destinataire.role("DAF"), 2)));
    assertThat(service.toutLire(rm1)).isEqualTo(2);
    assertThat(service.compterNonLues(rm1)).isZero();
    assertThat(service.compterNonLues(daf)).isEqualTo(1);
  }

  @Test
  void should_ignore_older_directory_versions() {
    FakeRepositories.Annuaire annuaire = new FakeRepositories.Annuaire();
    AnnuaireService annuaireService = new AnnuaireService(annuaire);
    UUID id = UUID.randomUUID();
    UtilisateurAnnuaire recent =
        new UtilisateurAnnuaire(id, "x@cse.sn", "A", "B", "DAF", Set.of(), true, Fixtures.T0);
    UtilisateurAnnuaire ancien =
        new UtilisateurAnnuaire(
            id, "old@cse.sn", "A", "B", "DAF", Set.of(), true, Fixtures.T0.minusSeconds(5));
    assertThat(annuaireService.synchroniser(recent)).isTrue();
    assertThat(annuaireService.synchroniser(ancien)).isFalse();
    assertThat(annuaire.parId(id))
        .get()
        .extracting(UtilisateurAnnuaire::email)
        .isEqualTo("x@cse.sn");
  }

  private static CurrentUser user(String role) {
    return new CurrentUser(UUID.randomUUID(), "U", "u@cse.sn", Set.of(role), Set.of(), null);
  }

  private static Notification notif(Destinataire d, int secondes) {
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
        Fixtures.T0.plusSeconds(secondes));
  }
}
