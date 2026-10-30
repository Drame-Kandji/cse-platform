package sn.cse.fournisseurs.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.fournisseurs.infrastructure.persistence.entity.PrixHistoriqueEntity;

/** Dépôt Spring Data de l'historique des prix. */
public interface PrixHistoriqueJpaRepository extends JpaRepository<PrixHistoriqueEntity, UUID> {

  /**
   * @param fournisseurId fournisseur
   * @return historique complet, du plus récent au plus ancien
   */
  List<PrixHistoriqueEntity> findByFournisseurIdOrderByDatePrixDesc(UUID fournisseurId);

  /**
   * @param fournisseurId fournisseur
   * @param reference référence
   * @return historique de la référence, du plus récent au plus ancien
   */
  List<PrixHistoriqueEntity> findByFournisseurIdAndReferenceOrderByDatePrixDesc(
      UUID fournisseurId, String reference);
}
