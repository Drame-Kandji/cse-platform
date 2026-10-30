package sn.cse.suivi.application.port.in;

import sn.cse.suivi.application.command.ConfirmerReceptionCommande;
import sn.cse.suivi.application.query.AffectationVue;

/** Confirmation de réception par le chef de chantier. */
public interface ConfirmerReceptionUseCase {

  /**
   * @param commande demande
   * @return l'affectation passée {@code EN_COURS}
   */
  AffectationVue confirmer(ConfirmerReceptionCommande commande);
}
