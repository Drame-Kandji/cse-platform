package sn.cse.suivi.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import sn.cse.suivi.application.port.out.TypeEnginRepository;
import sn.cse.suivi.domain.model.TypeEngin;
import sn.cse.suivi.infrastructure.persistence.mapper.PersistenceMapper;
import sn.cse.suivi.infrastructure.persistence.repository.TypeEnginJpaRepository;

/** Adaptateur JPA du référentiel des types d'engins. */
@Component
public class TypeEnginRepositoryAdapter implements TypeEnginRepository {

  private final TypeEnginJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public TypeEnginRepositoryAdapter(TypeEnginJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public List<TypeEngin> findAll() {
    return jpa.findAllByOrderByLibelleAsc().stream().map(PersistenceMapper::versDomaine).toList();
  }

  @Override
  public Optional<TypeEngin> findById(UUID id) {
    return jpa.findById(id).map(PersistenceMapper::versDomaine);
  }

  @Override
  public TypeEngin save(TypeEngin type) {
    return PersistenceMapper.versDomaine(jpa.saveAndFlush(PersistenceMapper.versEntite(type)));
  }

  @Override
  public boolean existsByCode(String code, UUID exclureId) {
    return exclureId == null ? jpa.existsByCode(code) : jpa.existsByCodeAndIdNot(code, exclureId);
  }
}
