package sn.cse.achats.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.achats.infrastructure.persistence.entity.BesoinAAcheterEntity;

/** Accès Spring Data à la projection {@code besoin_a_acheter}. */
public interface BesoinAAcheterJpaRepository extends JpaRepository<BesoinAAcheterEntity, UUID> {}
