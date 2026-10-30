package sn.cse.besoins.application.port.in;

import sn.cse.besoins.application.command.SoumettreBesoinCommand;
import sn.cse.besoins.application.query.BesoinVue;

/**
 * Soumission d'un besoin par un chef de chantier (sur ses chantiers) ou le Responsable Matériel.
 */
public interface SoumettreBesoinUseCase {

  /**
   * @param commande commande
   * @return le besoin après l'opération
   */
  BesoinVue soumettre(SoumettreBesoinCommand commande);
}
