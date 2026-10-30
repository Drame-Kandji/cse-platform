package sn.cse.fournisseurs.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.cse.fournisseurs.domain.model.StatutLitige;
import sn.cse.fournisseurs.infrastructure.persistence.entity.LitigeEntity;

/** Dépôt Spring Data des litiges. */
public interface LitigeJpaRepository
    extends JpaRepository<LitigeEntity, UUID>, JpaSpecificationExecutor<LitigeEntity> {

  /**
   * @param ids fournisseurs
   * @return triplets [fournisseurId, statut, nombre]
   */
  @Query(
      "select l.fournisseurId, l.statut, count(l) from LitigeEntity l"
          + " where l.fournisseurId in :ids group by l.fournisseurId, l.statut")
  List<Object[]> compterParFournisseurEtStatut(@Param("ids") Collection<UUID> ids);

  /**
   * @param statuts statuts
   * @return nombre de litiges dans ces statuts
   */
  long countByStatutIn(Collection<StatutLitige> statuts);
}
