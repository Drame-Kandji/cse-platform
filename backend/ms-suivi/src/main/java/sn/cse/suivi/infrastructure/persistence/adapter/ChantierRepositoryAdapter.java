package sn.cse.suivi.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.web.PageableFactory;
import sn.cse.suivi.application.port.out.ChantierRepository;
import sn.cse.suivi.application.query.FiltreChantiers;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.infrastructure.persistence.entity.ChantierEntity;
import sn.cse.suivi.infrastructure.persistence.mapper.PersistenceMapper;
import sn.cse.suivi.infrastructure.persistence.repository.ChantierJpaRepository;

/** Adaptateur JPA des chantiers. */
@Component
public class ChantierRepositoryAdapter implements ChantierRepository {

  private final ChantierJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public ChantierRepositoryAdapter(ChantierJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Chantier> findById(UUID id) {
    return jpa.findById(id).map(PersistenceMapper::versDomaine);
  }

  @Override
  public Chantier save(Chantier chantier) {
    return PersistenceMapper.versDomaine(jpa.saveAndFlush(PersistenceMapper.versEntite(chantier)));
  }

  @Override
  public boolean existsByCode(String code, UUID exclureId) {
    return exclureId == null ? jpa.existsByCode(code) : jpa.existsByCodeAndIdNot(code, exclureId);
  }

  @Override
  public PageResponse<Chantier> rechercher(FiltreChantiers filtre, PageQuery page) {
    var spec =
        Specs.<ChantierEntity>tous(
            Specs.egal("statut", filtre.statut()),
            Specs.contient(filtre.q(), "code", "nom", "localisation", "region"),
            Specs.perimetre("id", filtre.perimetre()));
    return Specs.page(
        jpa.findAll(spec, PageableFactory.toPageable(page)), PersistenceMapper::versDomaine);
  }

  @Override
  public List<Chantier> findAllById(Collection<UUID> ids) {
    return jpa.findAllById(ids).stream().map(PersistenceMapper::versDomaine).toList();
  }

  @Override
  public List<Chantier> findAll() {
    return jpa.findAll(Sort.by("code")).stream().map(PersistenceMapper::versDomaine).toList();
  }
}
