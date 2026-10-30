package sn.cse.achats.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.achats.infrastructure.persistence.entity.OffreEntity;

import java.util.List;
import java.util.UUID;

public interface OffreJpaRepository extends JpaRepository<OffreEntity, UUID> {
    List<OffreEntity> findByAppelOffreId(UUID appelOffreId);
}

