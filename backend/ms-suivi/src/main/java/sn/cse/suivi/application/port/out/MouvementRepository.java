package sn.cse.suivi.application.port.out;

import java.util.List;
import java.util.UUID;
import sn.cse.suivi.domain.model.Mouvement;

/** Journal des mouvements des matériels (ajout seul). */
public interface MouvementRepository {

  /**
   * @param mouvement mouvement à tracer
   */
  void ajouter(Mouvement mouvement);

  /**
   * @param materielId matériel
   * @param limite nombre maximal
   * @return derniers mouvements (plus récents d'abord)
   */
  List<Mouvement> findRecentsByMateriel(UUID materielId, int limite);
}
