package sn.cse.besoins.application.port.in;

import sn.cse.besoins.application.command.ModifierBesoinCommand;
import sn.cse.besoins.application.query.BesoinVue;

/** Modification d'un besoin par son demandeur tant qu'il est en attente (R05). */
public interface ModifierBesoinUseCase {

  /**
   * @param commande commande
   * @return le besoin après l'opération
   */
  BesoinVue modifier(ModifierBesoinCommand commande);
}
