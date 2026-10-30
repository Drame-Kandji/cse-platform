package sn.cse.besoins.application.port.in;

import sn.cse.besoins.application.command.JournaliserAffectationCommand;

/** Trace dans l'historique d'un besoin une affectation notifiée par ms-suivi. */
public interface JournaliserAffectationUseCase {

  /**
   * @param commande affectation notifiée
   * @return vrai si un besoin a été historisé, faux si le besoin est inconnu
   */
  boolean journaliser(JournaliserAffectationCommand commande);
}
