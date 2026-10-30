package sn.cse.suivi.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.suivi.application.query.FiltrePannes;
import sn.cse.suivi.domain.model.Panne;

/** Stockage des pannes. */
public interface PanneRepository {

  /**
   * @param panne panne
   * @return l'état enregistré
   */
  Panne save(Panne panne);

  /**
   * @param id identifiant
   * @return la panne si elle existe
   */
  Optional<Panne> findById(UUID id);

  /**
   * @param filtre critères
   * @param page pagination
   * @return la page de pannes
   */
  PageResponse<Panne> rechercher(FiltrePannes filtre, PageQuery page);

  /**
   * @param materielId matériel
   * @param limite nombre maximal
   * @return dernières pannes du matériel
   */
  List<Panne> findRecentesByMateriel(UUID materielId, int limite);
}
