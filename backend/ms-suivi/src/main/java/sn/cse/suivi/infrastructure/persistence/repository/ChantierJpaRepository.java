package sn.cse.suivi.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.suivi.infrastructure.persistence.entity.ChantierEntity;

import java.util.UUID;

public interface ChantierJpaRepository extends JpaRepository<ChantierEntity, UUID> {
}
