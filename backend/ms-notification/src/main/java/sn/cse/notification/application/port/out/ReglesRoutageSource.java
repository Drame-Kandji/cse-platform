package sn.cse.notification.application.port.out;

import java.util.List;
import java.util.Map;
import sn.cse.notification.domain.model.RegleRoutage;

/** Source des règles de routage (configuration). */
public interface ReglesRoutageSource {

  /**
   * @return les règles, dans l'ordre de déclaration
   */
  List<RegleRoutage> regles();

  /**
   * @return dictionnaire code → libellé utilisé dans les textes
   */
  Map<String, String> libelles();

  /**
   * @return texte de repli d'un marqueur introuvable
   */
  String valeurAbsente();
}
