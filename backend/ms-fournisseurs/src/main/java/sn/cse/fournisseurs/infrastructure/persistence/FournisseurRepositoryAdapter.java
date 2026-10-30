package sn.cse.fournisseurs.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.cse.fournisseurs.application.port.out.FournisseurRepositoryPort;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.infrastructure.persistence.entity.FournisseurEntity;
import sn.cse.fournisseurs.infrastructure.persistence.repository.FournisseurJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FournisseurRepositoryAdapter implements FournisseurRepositoryPort {

    private final FournisseurJpaRepository jpaRepository;

    @Override
    public List<Fournisseur> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Fournisseur> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Fournisseur save(Fournisseur f) {
        FournisseurEntity e = jpaRepository.findById(f.getId())
                .orElse(FournisseurEntity.builder().id(f.getId()).createdAt(Instant.now()).build());
        e.setRaisonSociale(f.getRaisonSociale());
        e.setNinea(f.getNinea());
        e.setRc(f.getRc());
        e.setEmail(f.getEmail());
        e.setTelephone(f.getTelephone());
        e.setSpecialite(f.getSpecialite());
        e.setStatut(f.getStatut());
        e.setScorePerformance(f.getScorePerformance());
        e.setDemandeBlacklistage(f.isDemandeBlacklistage());
        e.setMotifBlacklistage(f.getMotifBlacklistage());
        e.setUpdatedAt(Instant.now());
        return toDomain(jpaRepository.save(e));
    }

    private Fournisseur toDomain(FournisseurEntity e) {
        return new Fournisseur(
                e.getId(),
                e.getRaisonSociale(),
                e.getNinea(),
                e.getRc(),
                e.getEmail(),
                e.getTelephone(),
                e.getSpecialite(),
                e.getStatut(),
                e.getScorePerformance() != null ? e.getScorePerformance() : 80.0,
                Boolean.TRUE.equals(e.getDemandeBlacklistage()),
                e.getMotifBlacklistage(),
                e.getCreatedAt()
        );
    }
}

