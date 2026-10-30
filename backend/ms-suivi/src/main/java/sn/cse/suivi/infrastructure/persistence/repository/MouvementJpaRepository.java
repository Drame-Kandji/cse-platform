package sn.cse.suivi.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.suivi.infrastructure.persistence.entity.MouvementEntity;

/** Accès JPA au journal des mouvements. */
public interface MouvementJpaRepository extends JpaRepository<MouvementEntity, UUID> {

  /**
   * @param materielId matériel
   * @param pageable limite
   * @return derniers mouvements
   */
  List<MouvementEntity> findByMaterielIdOrderByDateDesc(UUID materielId, Pageable pageable);
}
