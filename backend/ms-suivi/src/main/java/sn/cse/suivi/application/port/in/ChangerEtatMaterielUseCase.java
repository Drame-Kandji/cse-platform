package sn.cse.suivi.application.port.in;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.domain.model.EtatMateriel;

/** Changement d'état manuel (mise en maintenance / remise en service). */
public interface ChangerEtatMaterielUseCase {

  /**
   * @param materielId matériel
   * @param etat état demandé ({@code DISPONIBLE} ou {@code EN_MAINTENANCE})
   * @param motif motif
   * @param auteur utilisateur
   * @return le matériel mis à jour
   */
  MaterielVue changerEtat(UUID materielId, EtatMateriel etat, String motif, CurrentUser auteur);
}
