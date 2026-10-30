package sn.cse.suivi.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sn.cse.suivi.domain.model.StatutAffectation;
import sn.cse.suivi.infrastructure.persistence.entity.AffectationEntity;

/** Accès JPA aux affectations. */
public interface AffectationJpaRepository
    extends JpaRepository<AffectationEntity, UUID>, JpaSpecificationExecutor<AffectationEntity> {

  /**
   * @param materielId matériel
   * @param statuts statuts recherchés
   * @return affectations du matériel dans ces statuts
   */
  List<AffectationEntity> findByMaterielIdAndStatutIn(
      UUID materielId, Collection<StatutAffectation> statuts);

  /**
   * @param materielId matériel
   * @param pageable limite
   * @return dernières affectations
   */
  List<AffectationEntity> findByMaterielIdOrderByDateDebutDesc(UUID materielId, Pageable pageable);

  /**
   * @param statuts statuts actifs
   * @param limite fin prévue maximale
   * @return affectations actives non encore alertées finissant au plus tard à la limite
   */
  List<AffectationEntity> findByStatutInAndDateFinPrevueLessThanEqualAndAlerteFinEnvoyeeFalse(
      Collection<StatutAffectation> statuts, LocalDate limite);

  /**
   * @param chantierId chantier
   * @param statuts statuts actifs
   * @return nombre d'affectations actives
   */
  long countByChantierIdAndStatutIn(UUID chantierId, Collection<StatutAffectation> statuts);
}
