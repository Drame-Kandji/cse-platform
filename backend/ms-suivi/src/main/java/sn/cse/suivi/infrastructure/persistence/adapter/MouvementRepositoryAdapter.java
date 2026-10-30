package sn.cse.suivi.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.infrastructure.persistence.mapper.PersistenceMapper;
import sn.cse.suivi.infrastructure.persistence.repository.MouvementJpaRepository;

/** Adaptateur JPA du journal des mouvements (insertion directe, sans fusion). */
@Component
public class MouvementRepositoryAdapter implements MouvementRepository {

  private final MouvementJpaRepository jpa;
  private final EntityManager entityManager;

  /**
   * @param jpa dépôt Spring Data
   * @param entityManager gestionnaire d'entités (persistance directe)
   */
  public MouvementRepositoryAdapter(MouvementJpaRepository jpa, EntityManager entityManager) {
    this.jpa = jpa;
    this.entityManager = entityManager;
  }

  @Override
  public void ajouter(Mouvement mouvement) {
    entityManager.persist(PersistenceMapper.versEntite(mouvement));
  }

  @Override
  public List<Mouvement> findRecentsByMateriel(UUID materielId, int limite) {
    return jpa.findByMaterielIdOrderByDateDesc(materielId, PageRequest.of(0, limite)).stream()
        .map(PersistenceMapper::versDomaine)
        .toList();
  }
}
