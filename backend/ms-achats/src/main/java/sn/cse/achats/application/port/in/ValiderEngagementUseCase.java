package sn.cse.achats.application.port.in;

import sn.cse.achats.application.command.DecisionCommand;
import sn.cse.achats.domain.model.ContratAchat;

/** Décision de la DAF sur l'engagement d'un contrat signé (R06). */
public interface ValiderEngagementUseCase {

  /**
   * @param commande décision (cible = contrat)
   * @return le contrat
   */
  ContratAchat decider(DecisionCommand commande);
}
