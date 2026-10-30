package sn.cse.suivi.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.cse.suivi.infrastructure.persistence.entity.MaterielEntity;

/** Accès JPA aux matériels. */
public interface MaterielJpaRepository
    extends JpaRepository<MaterielEntity, UUID>, JpaSpecificationExecutor<MaterielEntity> {

  /**
   * @param code code
   * @return vrai si le code existe
   */
  boolean existsByCode(String code);

  /**
   * @param code code
   * @param id identifiant exclu
   * @return vrai si un autre matériel porte le code
   */
  boolean existsByCodeAndIdNot(String code, UUID id);

  /**
   * @param typeEnginId type
   * @return matériels du type
   */
  List<MaterielEntity> findByTypeEnginId(UUID typeEnginId);

  /**
   * @param ids chantiers
   * @return couples (chantier, nombre de matériels présents)
   */
  @Query(
      "select m.chantierActuelId, count(m) from MaterielEntity m"
          + " where m.chantierActuelId in :ids group by m.chantierActuelId")
  List<Object[]> compterParChantier(@Param("ids") Collection<UUID> ids);
}
