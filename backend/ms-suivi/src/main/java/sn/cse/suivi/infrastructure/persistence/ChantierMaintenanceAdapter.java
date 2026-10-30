package sn.cse.suivi.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.cse.suivi.application.port.out.ChantierMaintenancePort;
import sn.cse.suivi.infrastructure.persistence.entity.ChantierEntity;
import sn.cse.suivi.infrastructure.persistence.repository.ChantierJpaRepository;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChantierMaintenanceAdapter implements ChantierMaintenancePort {

    private final ChantierJpaRepository chantierJpaRepository;

    @Override
    public List<ChantierDto> findAllChantiers() {
        return chantierJpaRepository.findAll().stream()
                .map(c -> new ChantierDto(
                        c.getId(), c.getNom(), c.getAdresse(), c.getVille(),
                        c.getLatitude(), c.getLongitude(), c.getStatut(), c.getBudget()
                ))
                .toList();
    }

    @Override
    public ChantierDto saveChantier(ChantierDto dto) {
        ChantierEntity entity = ChantierEntity.builder()
                .id(dto.id())
                .nom(dto.nom())
                .adresse(dto.adresse())
                .ville(dto.ville())
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .statut(dto.statut() != null ? dto.statut() : "EN_COURS")
                .budget(dto.budget())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        ChantierEntity saved = chantierJpaRepository.save(entity);
        return new ChantierDto(
                saved.getId(), saved.getNom(), saved.getAdresse(), saved.getVille(),
                saved.getLatitude(), saved.getLongitude(), saved.getStatut(), saved.getBudget()
        );
    }
}

