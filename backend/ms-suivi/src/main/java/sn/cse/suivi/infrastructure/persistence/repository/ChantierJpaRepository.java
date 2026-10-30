package sn.cse.suivi.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sn.cse.suivi.infrastructure.persistence.entity.ChantierEntity;

/** Accès JPA aux chantiers. */
public interface ChantierJpaRepository
    extends JpaRepository<ChantierEntity, UUID>, JpaSpecificationExecutor<ChantierEntity> {

  /**
   * @param code code
   * @return vrai si le code existe
   */
  boolean existsByCode(String code);

  /**
   * @param code code
   * @param id identifiant exclu
   * @return vrai si un autre chantier porte le code
   */
  boolean existsByCodeAndIdNot(String code, UUID id);
}
