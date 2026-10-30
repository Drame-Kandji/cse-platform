package sn.cse.fournisseurs.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.fournisseurs.infrastructure.persistence.entity.EvaluationEntity;

/** Dépôt Spring Data des évaluations. */
public interface EvaluationJpaRepository extends JpaRepository<EvaluationEntity, UUID> {

  /**
   * @param fournisseurId fournisseur
   * @return évaluations de la plus récente à la plus ancienne
   */
  List<EvaluationEntity> findByFournisseurIdOrderByDateEvaluationDesc(UUID fournisseurId);
}
