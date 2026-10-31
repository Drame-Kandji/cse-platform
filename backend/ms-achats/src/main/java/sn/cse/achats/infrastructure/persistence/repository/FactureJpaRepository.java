package sn.cse.achats.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sn.cse.achats.infrastructure.persistence.entity.FactureEntity;

/** Accès Spring Data à la table {@code facture}. */
public interface FactureJpaRepository
    extends JpaRepository<FactureEntity, UUID>, JpaSpecificationExecutor<FactureEntity> {

  /**
   * @param contratId contrat
   * @return ses factures
   */
  List<FactureEntity> findByContratId(UUID contratId);

  /**
   * @param fournisseurId fournisseur
   * @param numero numéro
   * @return vrai si la facture existe
   */
  boolean existsByFournisseurIdAndNumero(UUID fournisseurId, String numero);
}
