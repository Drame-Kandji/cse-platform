package sn.cse.achats.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.achats.infrastructure.persistence.entity.FournisseurProjectionEntity;

/** Accès Spring Data à la projection {@code fournisseur_projection}. */
public interface FournisseurProjectionJpaRepository
    extends JpaRepository<FournisseurProjectionEntity, UUID> {}
