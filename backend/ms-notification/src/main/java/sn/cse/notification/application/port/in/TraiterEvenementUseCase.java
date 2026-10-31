package sn.cse.notification.application.port.in;

import java.util.List;
import sn.cse.notification.domain.model.EvenementMetier;
import sn.cse.notification.domain.model.Notification;

/** Transforme un événement métier en notifications selon les règles de routage. */
public interface TraiterEvenementUseCase {

  /**
   * @param evenement événement reçu
   * @return les notifications créées (vide si aucune règle ne s'applique)
   */
  List<Notification> traiter(EvenementMetier evenement);
}
