package sn.cse.suivi.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.cse.suivi.application.port.out.MaterielRepositoryPort;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.infrastructure.persistence.entity.MaterielEntity;
import sn.cse.suivi.infrastructure.persistence.mapper.MaterielMapper;
import sn.cse.suivi.infrastructure.persistence.repository.MaterielJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MaterielRepositoryAdapter implements MaterielRepositoryPort {

    private final MaterielJpaRepository jpaRepository;
    private final MaterielMapper mapper;

    @Override
    public Materiel save(Materiel materiel) {
        MaterielEntity entity = jpaRepository.findById(materiel.getId())
                .orElse(MaterielEntity.builder().id(materiel.getId()).build());
        entity.setImmatriculation(materiel.getImmatriculation());
        entity.setDesignation(materiel.getDesignation());
        entity.setCategorie(materiel.getCategorie());
        entity.setEtat(materiel.getEtat());
        entity.setHeuresFonctionnement(materiel.getHeuresFonctionnement());
        entity.setChantierActuelId(materiel.getChantierActuelId());
        if (entity.getCreatedAt() == null) entity.setCreatedAt(java.time.Instant.now());
        entity.setUpdatedAt(java.time.Instant.now());
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Materiel> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Materiel> findByImmatriculation(String immatriculation) {
        return jpaRepository.findByImmatriculation(immatriculation).map(mapper::toDomain);
    }

    @Override
    public List<Materiel> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Materiel> findByEtat(EtatMateriel etat) {
        return jpaRepository.findByEtat(etat).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Materiel> findByCategorie(String categorie) {
        return jpaRepository.findByCategorie(categorie).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countByEtatAndCategorie(EtatMateriel etat, String categorie) {
        return jpaRepository.findByEtat(etat).stream().filter(m -> categorie.equals(m.getCategorie())).count();
    }
}
