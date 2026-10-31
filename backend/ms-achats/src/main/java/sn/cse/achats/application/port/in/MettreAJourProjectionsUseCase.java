package sn.cse.achats.application.port.in;

import sn.cse.achats.application.command.BesoinEnAchatCommand;
import sn.cse.achats.application.command.FournisseurObserveCommand;

/** Alimentation des projections locales à partir des événements des autres services. */
public interface MettreAJourProjectionsUseCase {

  /**
   * @param commande besoin orienté vers l'achat
   */
  void besoinEnAchat(BesoinEnAchatCommand commande);

  /**
   * @param commande nouvel état d'un fournisseur
   */
  void fournisseurObserve(FournisseurObserveCommand commande);
}
