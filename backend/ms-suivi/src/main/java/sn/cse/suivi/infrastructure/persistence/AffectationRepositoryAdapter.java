package sn.cse.suivi.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.cse.suivi.application.port.out.AffectationRepositoryPort;
import sn.cse.suivi.infrastructure.persistence.entity.AffectationEntity;
import sn.cse.suivi.infrastructure.persistence.repository.AffectationJpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AffectationRepositoryAdapter implements AffectationRepositoryPort {

    private final AffectationJpaRepository jpaRepository;

    @Override
    public boolean hasActiveConflict(UUID materielId) {
        return !jpaRepository.findByMaterielIdAndDateFinReelleIsNull(materielId).isEmpty();
    }

    @Override
    public void saveAffectation(UUID id, UUID materielId, UUID chantierId, UUID besoinId, UUID demandeurId,
                                 LocalDate dateDebut, LocalDate dateFinPrevue, String motif, String affectePar) {
        AffectationEntity entity = AffectationEntity.builder()
                .id(id)
                .materielId(materielId)
                .chantierId(chantierId)
                .besoinId(besoinId)
                .demandeurId(demandeurId)
                .dateDebut(dateDebut)
                .dateFinPrevue(dateFinPrevue)
                .motif(motif)
                .affectePar(affectePar)
                .createdAt(Instant.now())
                .build();
        jpaRepository.save(entity);
    }
}
