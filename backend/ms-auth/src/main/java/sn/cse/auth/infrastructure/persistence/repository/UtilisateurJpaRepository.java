package sn.cse.auth.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.auth.infrastructure.persistence.entity.UtilisateurEntity;

import java.util.UUID;

public interface UtilisateurJpaRepository extends JpaRepository<UtilisateurEntity, UUID> {
}

