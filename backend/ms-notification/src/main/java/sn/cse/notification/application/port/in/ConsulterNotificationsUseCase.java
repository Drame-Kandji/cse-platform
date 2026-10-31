package sn.cse.notification.application.port.in;

import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.notification.domain.model.NotificationVue;

/**
 * Consultation et lecture des notifications de l'utilisateur courant (adressées à lui ou à l'un de
 * ses rôles).
 */
public interface ConsulterNotificationsUseCase {

  /**
   * @param utilisateur utilisateur courant
   * @param lue filtre sur l'état de lecture ({@code null} = toutes)
   * @param page pagination
   * @return la page de notifications, les plus récentes d'abord
   */
  PageResponse<NotificationVue> lister(CurrentUser utilisateur, Boolean lue, PageQuery page);

  /**
   * @param utilisateur utilisateur courant
   * @return le nombre de notifications non lues
   */
  long compterNonLues(CurrentUser utilisateur);

  /**
   * @param utilisateur utilisateur courant
   * @param notificationId notification visée
   * @return la notification marquée comme lue
   * @throws sn.cse.common.exception.ResourceNotFoundException si elle n'existe pas ou ne lui est
   *     pas destinée
   */
  NotificationVue marquerLue(CurrentUser utilisateur, UUID notificationId);

  /**
   * @param utilisateur utilisateur courant
   * @return le nombre de notifications nouvellement marquées comme lues
   */
  int toutLire(CurrentUser utilisateur);
}
