package sn.cse.notification.application.service;

import java.util.List;
import sn.cse.notification.domain.model.Notification;

/**
 * Événement applicatif publié après la création de notifications ; traité après validation de la
 * transaction pour ne jamais diffuser (SSE, e-mail) une notification annulée.
 *
 * @param notifications notifications créées, avec l'indication d'envoi d'e-mail
 */
public record NotificationsCreees(List<Creee> notifications) {

  public NotificationsCreees {
    notifications = List.copyOf(notifications);
  }

  /**
   * @param notification notification enregistrée
   * @param email vrai si un e-mail doit aussi être envoyé
   */
  public record Creee(Notification notification, boolean email) {}
}
