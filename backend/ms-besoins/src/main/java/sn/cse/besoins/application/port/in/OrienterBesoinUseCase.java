package sn.cse.besoins.application.port.in;

import sn.cse.besoins.application.command.OrienterBesoinCommand;
import sn.cse.besoins.application.query.BesoinVue;

/** Orientation d'un besoin validé : affectation directe si le parc suffit, sinon achat. */
public interface OrienterBesoinUseCase {

  /**
   * @param commande commande
   * @return le besoin après l'opération
   */
  BesoinVue orienter(OrienterBesoinCommand commande);
}
