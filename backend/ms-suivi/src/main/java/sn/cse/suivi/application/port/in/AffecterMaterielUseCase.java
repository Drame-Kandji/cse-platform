package sn.cse.suivi.application.port.in;

import sn.cse.suivi.application.command.AffecterMaterielCommande;
import sn.cse.suivi.application.query.AffectationVue;

/** Affectation d'un matériel à un chantier (R04, détection des chevauchements). */
public interface AffecterMaterielUseCase {

  /**
   * @param commande demande
   * @return l'affectation créée ({@code EN_ATTENTE_RECEPTION})
   */
  AffectationVue affecter(AffecterMaterielCommande commande);
}
