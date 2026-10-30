package sn.cse.suivi.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.suivi.infrastructure.persistence.entity.TypeEnginEntity;

/** Accès JPA aux types d'engins. */
public interface TypeEnginJpaRepository extends JpaRepository<TypeEnginEntity, UUID> {

  /**
   * @return tous les types par libellé
   */
  List<TypeEnginEntity> findAllByOrderByLibelleAsc();

  /**
   * @param code code
   * @return vrai si le code existe
   */
  boolean existsByCode(String code);

  /**
   * @param code code
   * @param id identifiant exclu
   * @return vrai si un autre type porte le code
   */
  boolean existsByCodeAndIdNot(String code, UUID id);
}
