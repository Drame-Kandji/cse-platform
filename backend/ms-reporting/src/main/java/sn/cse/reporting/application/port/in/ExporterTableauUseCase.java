package sn.cse.reporting.application.port.in;

import sn.cse.reporting.application.command.DemandeTableau;
import sn.cse.reporting.application.command.FichierExporte;

/** Export du tableau de bord en PDF ou Excel. */
public interface ExporterTableauUseCase {

  /**
   * @param demande profil, période et chantier
   * @param format {@code pdf} ou {@code xlsx}
   * @return le fichier
   */
  FichierExporte exporter(DemandeTableau demande, String format);
}
