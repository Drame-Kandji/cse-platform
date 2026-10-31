package sn.cse.achats.application.port.in;

import java.util.UUID;
import sn.cse.achats.domain.model.TypeProcesVerbal;

/** Production des procès-verbaux d'ouverture et d'évaluation. */
public interface GenererProcesVerbalUseCase {

  /**
   * @param appelOffreId appel d'offres
   * @param type type de procès-verbal
   * @return le PDF
   */
  DocumentPdf generer(UUID appelOffreId, TypeProcesVerbal type);
}
