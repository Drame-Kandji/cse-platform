package sn.cse.suivi.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sn.cse.suivi.infrastructure.persistence.entity.PanneEntity;

/** Accès JPA aux pannes. */
public interface PanneJpaRepository
    extends JpaRepository<PanneEntity, UUID>, JpaSpecificationExecutor<PanneEntity> {

  /**
   * @param materielId matériel
   * @param pageable limite
   * @return dernières pannes
   */
  List<PanneEntity> findByMaterielIdOrderByDateSignalementDesc(UUID materielId, Pageable pageable);
}
