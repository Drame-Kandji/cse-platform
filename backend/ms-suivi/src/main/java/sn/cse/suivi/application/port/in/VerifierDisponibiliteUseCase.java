package sn.cse.suivi.application.port.in;

import sn.cse.suivi.application.query.CritereDisponibilite;
import sn.cse.suivi.application.query.DisponibiliteVue;

/** Vérification de la disponibilité de matériels (orientation des besoins). */
public interface VerifierDisponibiliteUseCase {

  /**
   * @param critere critères
   * @return la disponibilité et les matériels proposés
   */
  DisponibiliteVue verifier(CritereDisponibilite critere);
}
