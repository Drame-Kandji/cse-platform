package sn.cse.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sn.cse.notification.application.port.out.MailPort;
import sn.cse.notification.application.port.out.MessageEmail;
import sn.cse.notification.application.port.out.NotificationDiffuseur;
import sn.cse.notification.application.service.DiffusionNotificationsService;
import sn.cse.notification.application.service.NotificationsCreees;
import sn.cse.notification.domain.Fixtures;
import sn.cse.notification.domain.model.Destinataire;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.domain.model.Notification;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;

class DiffusionNotificationsServiceTest {

  private final NotificationDiffuseur diffuseur = mock(NotificationDiffuseur.class);
  private final MailPort mail = mock(MailPort.class);
  private final FakeRepositories.Annuaire annuaire = new FakeRepositories.Annuaire();
  private final DiffusionNotificationsService service =
      new DiffusionNotificationsService(diffuseur, mail, annuaire);

  @Test
  void should_push_and_email_every_active_holder_of_role() {
    annuaire.enregistrer(u("DAF", true, "daf@cse.sn"));
    annuaire.enregistrer(u("DAF", true, "daf2@cse.sn"));
    annuaire.enregistrer(u("DAF", false, "inactif@cse.sn"));
    Notification n = notif(Destinataire.role("DAF"));

    service.apresCreation(new NotificationsCreees(List.of(new NotificationsCreees.Creee(n, true))));

    verify(diffuseur).diffuser(n);
    ArgumentCaptor<MessageEmail> mails = ArgumentCaptor.forClass(MessageEmail.class);
    verify(mail, times(2)).envoyer(mails.capture());
    assertThat(mails.getAllValues())
        .extracting(MessageEmail::adresse)
        .containsExactlyInAnyOrder("daf@cse.sn", "daf2@cse.sn");
    assertThat(mails.getAllValues().get(0).lien()).isEqualTo("/contrats/1");
  }

  @Test
  void should_email_individual_user_only_when_known_and_requested() {
    UtilisateurAnnuaire chef = u("CHEF_CHANTIER", true, "chef@cse.sn");
    annuaire.enregistrer(chef);
    service.apresCreation(
        new NotificationsCreees(
            List.of(
                new NotificationsCreees.Creee(notif(Destinataire.utilisateur(chef.id())), true),
                new NotificationsCreees.Creee(
                    notif(Destinataire.utilisateur(UUID.randomUUID())), true),
                new NotificationsCreees.Creee(notif(Destinataire.utilisateur(chef.id())), false))));
    verify(mail, times(1)).envoyer(any());
    verify(diffuseur, times(3)).diffuser(any());
  }

  @Test
  void should_continue_when_push_or_directory_fails() {
    when(diffuseur.diffuser(any())).thenThrow(new IllegalStateException("flux fermé"));
    FakeRepositories.Annuaire enPanne =
        new FakeRepositories.Annuaire() {
          @Override
          public List<UtilisateurAnnuaire> actifsParRole(String role) {
            throw new IllegalStateException("base indisponible");
          }
        };
    DiffusionNotificationsService fragile =
        new DiffusionNotificationsService(diffuseur, mail, enPanne);
    fragile.apresCreation(
        new NotificationsCreees(
            List.of(
                new NotificationsCreees.Creee(notif(Destinataire.role("DAF")), true),
                new NotificationsCreees.Creee(notif(Destinataire.role("DAF")), false))));
    verify(diffuseur, times(2)).diffuser(any());
    verify(mail, never()).envoyer(any());
  }

  private static UtilisateurAnnuaire u(String role, boolean actif, String email) {
    return new UtilisateurAnnuaire(
        UUID.randomUUID(), email, "P", "N", role, Set.of(), actif, Fixtures.T0);
  }

  private static Notification notif(Destinataire d) {
    return new Notification(
        UUID.randomUUID(),
        "ENGAGEMENT_DEMANDE",
        "t",
        "m",
        NiveauNotification.ALERTE,
        "/contrats/1",
        "CONTRAT",
        "1",
        d,
        UUID.randomUUID(),
        Fixtures.T0);
  }
}
