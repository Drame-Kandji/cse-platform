package sn.cse.achats.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.achats.infrastructure.persistence.entity.ContratAchatEntity;

import java.util.UUID;

public interface ContratAchatJpaRepository extends JpaRepository<ContratAchatEntity, UUID> {
}

