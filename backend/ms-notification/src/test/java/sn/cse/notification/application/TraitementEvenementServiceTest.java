package sn.cse.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import sn.cse.notification.application.port.out.ReglesRoutageSource;
import sn.cse.notification.application.service.NotificationsCreees;
import sn.cse.notification.application.service.TraitementEvenementService;
import sn.cse.notification.domain.Fixtures;
import sn.cse.notification.domain.model.Destinataire;
import sn.cse.notification.domain.model.Notification;
import sn.cse.notification.domain.model.RegleRoutage;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;

class TraitementEvenementServiceTest {

  private final FakeRepositories.Notifications notifications = new FakeRepositories.Notifications();
  private final FakeRepositories.Annuaire annuaire = new FakeRepositories.Annuaire();
  private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

  private TraitementEvenementService service(RegleRoutage... regles) {
    ReglesRoutageSource source =
        new ReglesRoutageSource() {
          @Override
          public List<RegleRoutage> regles() {
            return List.of(regles);
          }

          @Override
          public Map<String, String> libelles() {
            return Map.of();
          }

          @Override
          public String valeurAbsente() {
            return "-";
          }
        };
    return new TraitementEvenementService(
        source, notifications, annuaire, publisher, Clock.fixed(Fixtures.T0, ZoneOffset.UTC));
  }

  @Test
  void should_notify_chefs_of_chantier_from_directory_and_publish_event() {
    UUID chantier = UUID.randomUUID();
    UUID chef1 = UUID.randomUUID();
    UUID chef2 = UUID.randomUUID();
    annuaire.enregistrer(chef(chef1, chantier, true));
    annuaire.enregistrer(chef(chef2, chantier, true));
    annuaire.enregistrer(chef(UUID.randomUUID(), chantier, false));
    annuaire.enregistrer(chef(UUID.randomUUID(), UUID.randomUUID(), true));

    List<Notification> resultat =
        service(Fixtures.regleChefs("MATERIEL_AFFECTE"))
            .traiter(
                Fixtures.evenement(
                    "MATERIEL_AFFECTE",
                    Map.of("chantierId", chantier.toString(), "chefChantierId", chef1.toString())));

    assertThat(resultat)
        .extracting(Notification::destinataire)
        .containsExactlyInAnyOrder(
            Destinataire.utilisateur(chef1), Destinataire.utilisateur(chef2));
    assertThat(notifications.stock).hasSize(2);
    assertThat(resultat).allSatisfy(n -> assertThat(n.createdAt()).isEqualTo(Fixtures.T0));
    ArgumentCaptor<NotificationsCreees> evt = ArgumentCaptor.forClass(NotificationsCreees.class);
    verify(publisher).publishEvent(evt.capture());
    assertThat(evt.getValue().notifications())
        .hasSize(2)
        .allMatch(NotificationsCreees.Creee::email);
  }

  @Test
  void should_store_role_notification_once() {
    List<Notification> resultat =
        service(Fixtures.regleRole("BESOIN_SOUMIS", "RESPONSABLE_MATERIEL", "t"))
            .traiter(Fixtures.evenement("BESOIN_SOUMIS", Map.of()));
    assertThat(resultat)
        .singleElement()
        .extracting(Notification::destinataire)
        .isEqualTo(Destinataire.role("RESPONSABLE_MATERIEL"));
  }

  @Test
  void should_do_nothing_when_no_rule_applies() {
    assertThat(service().traiter(Fixtures.evenement("AUDIT", Map.of()))).isEmpty();
    assertThat(notifications.stock).isEmpty();
    verify(publisher, never()).publishEvent(any(Object.class));
  }

  private static UtilisateurAnnuaire chef(UUID id, UUID chantier, boolean actif) {
    return new UtilisateurAnnuaire(
        id, id + "@cse.sn", "Chef", "Test", "CHEF_CHANTIER", Set.of(chantier), actif, Fixtures.T0);
  }
}
