package sn.cse.fournisseurs.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.fournisseurs.infrastructure.persistence.entity.FournisseurEntity;

import java.util.UUID;

public interface FournisseurJpaRepository extends JpaRepository<FournisseurEntity, UUID> {
}

