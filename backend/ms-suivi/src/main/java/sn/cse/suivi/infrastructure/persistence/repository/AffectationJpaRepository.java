package sn.cse.suivi.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.suivi.infrastructure.persistence.entity.AffectationEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AffectationJpaRepository extends JpaRepository<AffectationEntity, UUID> {
    List<AffectationEntity> findByMaterielIdAndDateFinReelleIsNull(UUID materielId);
    boolean existsByMaterielIdAndDateFinReelleIsNullAndDateFinPrevueAfter(UUID materielId, LocalDate date);
}
