package sn.cse.notification.application.port.out;

import sn.cse.notification.domain.model.Notification;

/** Diffusion temps réel (SSE) aux utilisateurs connectés concernés. */
public interface NotificationDiffuseur {

  /**
   * @param notification notification à pousser
   * @return nombre de connexions ayant reçu la notification
   */
  int diffuser(Notification notification);
}
