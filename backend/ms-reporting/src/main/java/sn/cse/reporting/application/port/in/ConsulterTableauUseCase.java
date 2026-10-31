package sn.cse.reporting.application.port.in;

import sn.cse.reporting.application.command.DemandeTableau;
import sn.cse.reporting.domain.model.Tableau;

/** Tableau de bord d'un profil. */
public interface ConsulterTableauUseCase {

  /**
   * @param demande profil, période et chantier demandés
   * @return le tableau de bord
   */
  Tableau tableau(DemandeTableau demande);
}
