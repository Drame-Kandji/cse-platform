package sn.cse.besoins.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Accès Spring Data aux besoins. */
interface BesoinMaterielJpaRepository
    extends JpaRepository<BesoinMaterielEntity, UUID>,
        JpaSpecificationExecutor<BesoinMaterielEntity> {}
