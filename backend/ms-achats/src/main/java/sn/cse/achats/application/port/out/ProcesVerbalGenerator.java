package sn.cse.achats.application.port.out;

import java.time.Instant;
import java.util.List;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.TypeProcesVerbal;
import sn.cse.achats.domain.service.OffreEvaluee;

/** Production des procès-verbaux d'appel d'offres au format PDF. */
public interface ProcesVerbalGenerator {

  /**
   * @param type type de procès-verbal
   * @param appelOffre appel d'offres (clôturé ou attribué)
   * @param offres offres évaluées, triées par rang
   * @param genereLe instant de génération
   * @return le document PDF
   */
  byte[] generer(
      TypeProcesVerbal type, AppelOffre appelOffre, List<OffreEvaluee> offres, Instant genereLe);
}
