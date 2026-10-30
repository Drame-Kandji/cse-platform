package sn.cse.auth.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.cse.auth.application.port.in.GestionUtilisateurUseCase.UtilisateurDto;
import sn.cse.auth.application.port.out.UtilisateurRepositoryPort;
import sn.cse.auth.infrastructure.persistence.entity.UtilisateurEntity;
import sn.cse.auth.infrastructure.persistence.repository.UtilisateurJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UtilisateurRepositoryAdapter implements UtilisateurRepositoryPort {

    private final UtilisateurJpaRepository jpaRepository;

    @Override
    public List<UtilisateurDto> findAll() {
        return jpaRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public Optional<UtilisateurDto> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDto);
    }

    @Override
    public UtilisateurDto save(UtilisateurDto dto) {
        UtilisateurEntity entity = jpaRepository.findById(dto.id())
                .orElse(UtilisateurEntity.builder()
                        .id(dto.id())
                        .keycloakId("kc-" + dto.id())
                        .createdAt(Instant.now())
                        .build());
        entity.setNom(dto.nom());
        entity.setPrenom(dto.prenom());
        entity.setEmail(dto.email());
        entity.setTypeUtilisateur(dto.role());
        entity.setActif(dto.actif());
        entity.setChantierAttribueId(dto.chantierAttribueId());
        entity.setPerimetreParc(dto.perimetreParc());
        entity.setUpdatedAt(Instant.now());
        return toDto(jpaRepository.save(entity));
    }

    private UtilisateurDto toDto(UtilisateurEntity e) {
        return new UtilisateurDto(
                e.getId(),
                e.getNom(),
                e.getPrenom(),
                e.getEmail(),
                e.getTypeUtilisateur(),
                Boolean.TRUE.equals(e.getActif()),
                e.getChantierAttribueId(),
                e.getPerimetreParc()
        );
    }
}

