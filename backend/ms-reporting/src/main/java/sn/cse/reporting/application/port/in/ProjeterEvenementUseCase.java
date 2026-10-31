package sn.cse.reporting.application.port.in;

import sn.cse.reporting.application.command.EvenementRecu;

/** Projection d'un événement de domaine dans les vues de reporting (CQRS, D3). */
public interface ProjeterEvenementUseCase {

  /**
   * Applique l'événement aux vues. Idempotent par construction : les vues sont des instantanés
   * fusionnés par identifiant d'agrégat, rejouer un événement ne change rien.
   *
   * @param evenement événement reçu
   * @return vrai si le type est pris en charge, faux s'il est ignoré
   */
  boolean projeter(EvenementRecu evenement);
}
