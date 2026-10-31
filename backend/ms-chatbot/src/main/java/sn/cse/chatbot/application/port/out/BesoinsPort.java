package sn.cse.chatbot.application.port.out;

import java.util.List;
import sn.cse.chatbot.domain.model.BesoinInfo;

/** Lecture seule dans ms-besoins (JWT relayé : un chef ne voit que ses chantiers). */
public interface BesoinsPort {

  /**
   * @param q référence ou texte, ou {@code null}
   * @param statut statut ou {@code null}
   * @param limite nombre maximal
   * @return les besoins visibles
   */
  List<BesoinInfo> rechercher(String q, String statut, int limite);
}
