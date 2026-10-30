package sn.cse.suivi.application.port.in;

import sn.cse.suivi.application.command.SignalerPanneCommande;
import sn.cse.suivi.application.query.PanneVue;

/** Signalement d'une panne. */
public interface SignalerPanneUseCase {

  /**
   * @param commande demande
   * @return la panne signalée ({@code PANNE_SIGNALEE})
   */
  PanneVue signaler(SignalerPanneCommande commande);
}
