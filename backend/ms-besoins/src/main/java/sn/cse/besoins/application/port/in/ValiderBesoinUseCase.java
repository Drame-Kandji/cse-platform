package sn.cse.besoins.application.port.in;

import sn.cse.besoins.application.command.ValiderBesoinCommand;
import sn.cse.besoins.application.query.BesoinVue;

/**
 * Validation d'un besoin (R01) puis orientation automatique vers l'affectation directe ou l'achat.
 */
public interface ValiderBesoinUseCase {

  /**
   * @param commande commande
   * @return le besoin après l'opération
   */
  BesoinVue valider(ValiderBesoinCommand commande);
}
