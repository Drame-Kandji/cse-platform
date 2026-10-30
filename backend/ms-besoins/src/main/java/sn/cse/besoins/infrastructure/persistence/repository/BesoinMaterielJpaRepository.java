package sn.cse.besoins.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.besoins.infrastructure.persistence.entity.BesoinMaterielEntity;

import java.util.UUID;

public interface BesoinMaterielJpaRepository extends JpaRepository<BesoinMaterielEntity, UUID> {
}
