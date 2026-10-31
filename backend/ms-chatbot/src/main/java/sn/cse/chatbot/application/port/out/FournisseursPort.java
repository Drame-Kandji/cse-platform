package sn.cse.chatbot.application.port.out;

import java.util.List;
import sn.cse.chatbot.domain.model.FournisseurInfo;

/** Lecture seule dans ms-fournisseurs. */
public interface FournisseursPort {

  /**
   * @param categorie catégorie ou {@code null}
   * @param limite nombre maximal
   * @return le classement des fournisseurs actifs
   */
  List<FournisseurInfo> classement(String categorie, int limite);
}
