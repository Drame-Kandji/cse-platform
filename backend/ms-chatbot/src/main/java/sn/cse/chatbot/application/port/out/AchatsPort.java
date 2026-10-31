package sn.cse.chatbot.application.port.out;

import java.util.List;
import sn.cse.chatbot.domain.model.AppelOffreInfo;
import sn.cse.chatbot.domain.model.ContratInfo;

/** Lecture seule dans ms-achats. */
public interface AchatsPort {

  /**
   * @param q numéro ou texte, ou {@code null}
   * @param limite nombre maximal
   * @return les contrats
   */
  List<ContratInfo> rechercherContrats(String q, int limite);

  /**
   * @param q référence ou texte, ou {@code null}
   * @param limite nombre maximal
   * @return les appels d'offres
   */
  List<AppelOffreInfo> rechercherAppelsOffres(String q, int limite);
}
