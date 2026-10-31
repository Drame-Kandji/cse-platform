package sn.cse.notification.application.service;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.notification.application.port.in.ConsulterNotificationsUseCase;
import sn.cse.notification.application.port.out.NotificationRepository;
import sn.cse.notification.domain.model.Notification;
import sn.cse.notification.domain.model.NotificationVue;

/** Lecture des notifications, limitée à celles destinées à l'utilisateur ou à ses rôles. */
@Service
public class ConsultationNotificationsService implements ConsulterNotificationsUseCase {

  private final NotificationRepository notifications;
  private final Clock clock;

  /**
   * @param notifications persistance
   * @param clock horloge
   */
  public ConsultationNotificationsService(NotificationRepository notifications, Clock clock) {
    this.notifications = notifications;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<NotificationVue> lister(
      CurrentUser utilisateur, Boolean lue, PageQuery page) {
    PageResponse<Notification> brute =
        notifications.visibles(utilisateur.id(), utilisateur.roles(), lue, page);
    Set<UUID> lues =
        Boolean.TRUE.equals(lue)
            ? Set.of()
            : notifications.luesParmi(
                utilisateur.id(), brute.content().stream().map(Notification::id).toList());
    return brute.map(
        n -> new NotificationVue(n, Boolean.TRUE.equals(lue) || lues.contains(n.id())));
  }

  @Override
  @Transactional(readOnly = true)
  public long compterNonLues(CurrentUser utilisateur) {
    return notifications.compterNonLues(utilisateur.id(), utilisateur.roles());
  }

  @Override
  @Transactional
  public NotificationVue marquerLue(CurrentUser utilisateur, UUID notificationId) {
    Notification n =
        notifications
            .parId(notificationId)
            .filter(x -> x.estDestineeA(utilisateur.id(), utilisateur.roles()))
            // Une notification d'autrui est traitée comme inexistante (pas de fuite d'information).
            .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
    notifications.marquerLue(n.id(), utilisateur.id(), clock.instant());
    return new NotificationVue(n, true);
  }

  @Override
  @Transactional
  public int toutLire(CurrentUser utilisateur) {
    return notifications.toutLire(utilisateur.id(), utilisateur.roles(), clock.instant());
  }
}
