package sn.cse.besoins.application.port.out;

import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.EvenementBesoin;

/** Publication des événements BESOIN_* (outbox transactionnelle, dans la transaction courante). */
public interface BesoinEventPublisher {

  /**
   * @param besoin état du besoin après l'action
   * @param evenement événement de domaine
   */
  void publier(BesoinMateriel besoin, EvenementBesoin evenement);
}
