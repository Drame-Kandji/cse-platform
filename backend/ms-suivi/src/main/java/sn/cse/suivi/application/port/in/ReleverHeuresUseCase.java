package sn.cse.suivi.application.port.in;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.query.MaterielVue;

/** Relevé du compteur horaire. */
public interface ReleverHeuresUseCase {

  /**
   * @param materielId matériel
   * @param heures nouvelle valeur du compteur
   * @param auteur utilisateur (chef : matériel de ses chantiers)
   * @return le matériel mis à jour
   */
  MaterielVue relever(UUID materielId, long heures, CurrentUser auteur);
}
