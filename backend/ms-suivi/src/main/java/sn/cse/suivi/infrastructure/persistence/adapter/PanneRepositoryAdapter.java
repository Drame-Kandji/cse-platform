package sn.cse.suivi.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.web.PageableFactory;
import sn.cse.suivi.application.port.out.PanneRepository;
import sn.cse.suivi.application.query.FiltrePannes;
import sn.cse.suivi.domain.model.Panne;
import sn.cse.suivi.infrastructure.persistence.entity.PanneEntity;
import sn.cse.suivi.infrastructure.persistence.mapper.PersistenceMapper;
import sn.cse.suivi.infrastructure.persistence.repository.PanneJpaRepository;

/** Adaptateur JPA des pannes. */
@Component
public class PanneRepositoryAdapter implements PanneRepository {

  private final PanneJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public PanneRepositoryAdapter(PanneJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Panne save(Panne panne) {
    return PersistenceMapper.versDomaine(jpa.saveAndFlush(PersistenceMapper.versEntite(panne)));
  }

  @Override
  public Optional<Panne> findById(UUID id) {
    return jpa.findById(id).map(PersistenceMapper::versDomaine);
  }

  @Override
  public PageResponse<Panne> rechercher(FiltrePannes filtre, PageQuery page) {
    Specification<PanneEntity> spec =
        Specs.tous(
            Specs.egal("statut", filtre.statut()),
            Specs.egal("materielId", filtre.materielId()),
            Specs.perimetre("chantierId", filtre.perimetre()));
    return Specs.page(
        jpa.findAll(spec, PageableFactory.toPageable(page)), PersistenceMapper::versDomaine);
  }

  @Override
  public List<Panne> findRecentesByMateriel(UUID materielId, int limite) {
    return jpa
        .findByMaterielIdOrderByDateSignalementDesc(materielId, PageRequest.of(0, limite))
        .stream()
        .map(PersistenceMapper::versDomaine)
        .toList();
  }
}
