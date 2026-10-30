package sn.cse.besoins.application.port.in;

import sn.cse.besoins.application.command.RejeterBesoinCommand;
import sn.cse.besoins.application.query.BesoinVue;

/** Rejet motivé d'un besoin (R01, R08). */
public interface RejeterBesoinUseCase {

  /**
   * @param commande commande
   * @return le besoin après l'opération
   */
  BesoinVue rejeter(RejeterBesoinCommand commande);
}
