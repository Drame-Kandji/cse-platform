package sn.cse.suivi.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.infrastructure.persistence.entity.MaintenanceEntity;

/** Accès JPA aux ordres de travail. */
public interface MaintenanceJpaRepository
    extends JpaRepository<MaintenanceEntity, UUID>, JpaSpecificationExecutor<MaintenanceEntity> {

  /**
   * @param materielId matériel
   * @param pageable limite
   * @return derniers ordres de travail
   */
  List<MaintenanceEntity> findByMaterielIdOrderByDatePlanifieeDesc(
      UUID materielId, Pageable pageable);

  /**
   * @param materielId matériel
   * @param statut statut
   * @return vrai s'il existe un ordre de travail dans ce statut
   */
  boolean existsByMaterielIdAndStatut(UUID materielId, StatutMaintenance statut);

  /**
   * @param materielId matériel
   * @param statut statut
   * @param id ordre de travail exclu
   * @return vrai s'il existe un autre ordre de travail dans ce statut
   */
  boolean existsByMaterielIdAndStatutAndIdNot(UUID materielId, StatutMaintenance statut, UUID id);

  /**
   * @param materielId matériel
   * @param statuts statuts
   * @return vrai s'il existe un ordre de travail dans l'un des statuts
   */
  boolean existsByMaterielIdAndStatutIn(UUID materielId, Collection<StatutMaintenance> statuts);

  /**
   * @param ids matériels
   * @param statut statut
   * @param du début
   * @param au fin
   * @return matériels ayant un ordre de travail dans ce statut planifié dans la période
   */
  @Query(
      "select distinct m.materielId from MaintenanceEntity m where m.materielId in :ids"
          + " and m.statut = :statut and m.datePlanifiee between :du and :au")
  List<UUID> materielsPlanifies(
      @Param("ids") Collection<UUID> ids,
      @Param("statut") StatutMaintenance statut,
      @Param("du") LocalDate du,
      @Param("au") LocalDate au);
}
