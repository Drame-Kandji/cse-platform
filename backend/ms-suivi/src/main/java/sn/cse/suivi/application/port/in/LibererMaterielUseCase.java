package sn.cse.suivi.application.port.in;

import sn.cse.suivi.application.command.LibererMaterielCommande;
import sn.cse.suivi.application.query.AffectationVue;

/** Libération d'un matériel (fin d'affectation). */
public interface LibererMaterielUseCase {

  /**
   * @param commande demande
   * @return l'affectation terminée
   */
  AffectationVue liberer(LibererMaterielCommande commande);
}
