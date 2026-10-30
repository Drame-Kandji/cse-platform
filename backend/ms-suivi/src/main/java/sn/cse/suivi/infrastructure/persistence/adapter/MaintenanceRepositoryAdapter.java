package sn.cse.suivi.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.web.PageableFactory;
import sn.cse.suivi.application.port.out.MaintenanceRepository;
import sn.cse.suivi.application.query.FiltreMaintenances;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.infrastructure.persistence.entity.MaintenanceEntity;
import sn.cse.suivi.infrastructure.persistence.mapper.PersistenceMapper;
import sn.cse.suivi.infrastructure.persistence.repository.MaintenanceJpaRepository;

/** Adaptateur JPA des ordres de travail. */
@Component
public class MaintenanceRepositoryAdapter implements MaintenanceRepository {

  private static final Set<StatutMaintenance> OUVERTES =
      EnumSet.of(StatutMaintenance.PLANIFIEE, StatutMaintenance.EN_COURS);

  private final MaintenanceJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public MaintenanceRepositoryAdapter(MaintenanceJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Maintenance save(Maintenance maintenance) {
    return PersistenceMapper.versDomaine(
        jpa.saveAndFlush(PersistenceMapper.versEntite(maintenance)));
  }

  @Override
  public Optional<Maintenance> findById(UUID id) {
    return jpa.findById(id).map(PersistenceMapper::versDomaine);
  }

  @Override
  public PageResponse<Maintenance> rechercher(FiltreMaintenances filtre, PageQuery page) {
    Specification<MaintenanceEntity> spec =
        Specs.tous(
            Specs.egal("statut", filtre.statut()),
            Specs.egal("type", filtre.type()),
            Specs.egal("materielId", filtre.materielId()),
            Specs.perimetre("chantierId", filtre.perimetre()));
    return Specs.page(
        jpa.findAll(spec, PageableFactory.toPageable(page)), PersistenceMapper::versDomaine);
  }

  @Override
  public List<Maintenance> findRecentesByMateriel(UUID materielId, int limite) {
    return jpa
        .findByMaterielIdOrderByDatePlanifieeDesc(materielId, PageRequest.of(0, limite))
        .stream()
        .map(PersistenceMapper::versDomaine)
        .toList();
  }

  @Override
  public boolean existeEnCours(UUID materielId, UUID exclureId) {
    return exclureId == null
        ? jpa.existsByMaterielIdAndStatut(materielId, StatutMaintenance.EN_COURS)
        : jpa.existsByMaterielIdAndStatutAndIdNot(
            materielId, StatutMaintenance.EN_COURS, exclureId);
  }

  @Override
  public boolean existeOuverte(UUID materielId) {
    return jpa.existsByMaterielIdAndStatutIn(materielId, OUVERTES);
  }

  @Override
  public Set<UUID> materielsAvecMaintenancePlanifiee(
      Collection<UUID> materielIds, LocalDate du, LocalDate au) {
    if (materielIds.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(jpa.materielsPlanifies(materielIds, StatutMaintenance.PLANIFIEE, du, au));
  }
}
