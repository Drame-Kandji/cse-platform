package sn.cse.achats.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.achats.infrastructure.persistence.entity.AppelOffreEntity;

import java.util.UUID;

public interface AppelOffreJpaRepository extends JpaRepository<AppelOffreEntity, UUID> {
}

