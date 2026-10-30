package sn.cse.suivi.application.port.out;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.suivi.application.query.FiltreMaintenances;
import sn.cse.suivi.domain.model.Maintenance;

/** Stockage des ordres de travail. */
public interface MaintenanceRepository {

  /**
   * @param maintenance ordre de travail
   * @return l'état enregistré
   */
  Maintenance save(Maintenance maintenance);

  /**
   * @param id identifiant
   * @return l'ordre de travail s'il existe
   */
  Optional<Maintenance> findById(UUID id);

  /**
   * @param filtre critères
   * @param page pagination
   * @return la page d'ordres de travail
   */
  PageResponse<Maintenance> rechercher(FiltreMaintenances filtre, PageQuery page);

  /**
   * @param materielId matériel
   * @param limite nombre maximal
   * @return derniers ordres de travail du matériel
   */
  List<Maintenance> findRecentesByMateriel(UUID materielId, int limite);

  /**
   * @param materielId matériel
   * @param exclureId ordre de travail à ignorer, peut être {@code null}
   * @return vrai si un autre ordre de travail est en cours sur le matériel
   */
  boolean existeEnCours(UUID materielId, UUID exclureId);

  /**
   * @param materielId matériel
   * @return vrai si un ordre de travail planifié ou en cours existe
   */
  boolean existeOuverte(UUID materielId);

  /**
   * @param materielIds matériels candidats
   * @param du début de période
   * @param au fin de période
   * @return matériels ayant une maintenance planifiée dans la période
   */
  Set<UUID> materielsAvecMaintenancePlanifiee(
      Collection<UUID> materielIds, LocalDate du, LocalDate au);
}
