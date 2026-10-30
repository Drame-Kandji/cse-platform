package sn.cse.fournisseurs.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sn.cse.fournisseurs.infrastructure.persistence.entity.LigneCatalogueEntity;

/** Dépôt Spring Data des lignes de catalogue. */
public interface LigneCatalogueJpaRepository
    extends JpaRepository<LigneCatalogueEntity, UUID>,
        JpaSpecificationExecutor<LigneCatalogueEntity> {

  /**
   * @param fournisseurId fournisseur
   * @return lignes triées par référence
   */
  List<LigneCatalogueEntity> findByFournisseurIdOrderByReferenceAsc(UUID fournisseurId);

  /**
   * @param fournisseurId fournisseur
   * @param reference référence
   * @return ligne
   */
  Optional<LigneCatalogueEntity> findByFournisseurIdAndReference(
      UUID fournisseurId, String reference);
}
