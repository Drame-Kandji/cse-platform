package sn.cse.reporting.application.port.out;

import sn.cse.reporting.domain.model.FormatExport;
import sn.cse.reporting.domain.model.Tableau;

/** Mise en forme d'un tableau de bord dans un format de fichier. */
public interface ExportateurTableau {

  /**
   * @return format produit
   */
  FormatExport format();

  /**
   * @param tableau tableau de bord
   * @return octets du fichier
   */
  byte[] exporter(Tableau tableau);
}
