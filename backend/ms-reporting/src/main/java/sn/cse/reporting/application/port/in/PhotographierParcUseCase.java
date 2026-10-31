package sn.cse.reporting.application.port.in;

import java.time.LocalDate;

/** Instantané quotidien du parc par catégorie et chantier (série TAUX_DISPONIBILITE). */
public interface PhotographierParcUseCase {

  /**
   * Remplace l'instantané du jour par l'état courant du parc.
   *
   * @param jour jour photographié
   */
  void photographier(LocalDate jour);
}
