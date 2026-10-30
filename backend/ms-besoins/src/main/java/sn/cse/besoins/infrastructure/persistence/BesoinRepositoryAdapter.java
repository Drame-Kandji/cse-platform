package sn.cse.besoins.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.cse.besoins.application.port.out.BesoinRepositoryPort;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.infrastructure.persistence.entity.BesoinMaterielEntity;
import sn.cse.besoins.infrastructure.persistence.repository.BesoinMaterielJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BesoinRepositoryAdapter implements BesoinRepositoryPort {

    private final BesoinMaterielJpaRepository jpaRepository;

    @Override
    public BesoinMateriel save(BesoinMateriel besoin) {
        BesoinMaterielEntity entity = jpaRepository.findById(besoin.getId())
                .orElse(BesoinMaterielEntity.builder().id(besoin.getId()).build());
        entity.setReference(besoin.getReference());
        entity.setDesignation(besoin.getDesignation());
        entity.setQuantite(besoin.getQuantite());
        entity.setSpecifications(besoin.getSpecifications());
        entity.setPriorite(besoin.getPriorite());
        entity.setStatut(besoin.getStatut());
        entity.setChantierId(besoin.getChantierId());
        entity.setDemandeurId(besoin.getDemandeurId());
        entity.setJustification(besoin.getJustification());
        entity.setMotifRejet(besoin.getMotifRejet());
        if (entity.getCreatedAt() == null) entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        BesoinMaterielEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<BesoinMateriel> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<BesoinMateriel> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private BesoinMateriel toDomain(BesoinMaterielEntity e) {
        return BesoinMateriel.reconstituer(
                e.getId(),
                e.getReference(),
                e.getDesignation(),
                e.getQuantite() != null ? e.getQuantite() : 0,
                e.getSpecifications(),
                e.getPriorite(),
                e.getStatut(),
                e.getChantierId(),
                e.getDemandeurId(),
                e.getJustification(),
                e.getMotifRejet(),
                e.getCreatedAt()
        );
    }
}
