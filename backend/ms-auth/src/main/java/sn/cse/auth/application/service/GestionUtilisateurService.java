package sn.cse.auth.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.auth.application.port.in.GestionUtilisateurUseCase;
import sn.cse.auth.application.port.out.UtilisateurRepositoryPort;
import sn.cse.common.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GestionUtilisateurService implements GestionUtilisateurUseCase {

    private final UtilisateurRepositoryPort repositoryPort;

    @Override
    @Transactional(readOnly = true)
    public List<UtilisateurDto> listerTous() {
        return repositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UtilisateurDto> trouverParId(UUID id) {
        return repositoryPort.findById(id);
    }

    @Override
    @Transactional
    public UtilisateurDto creer(CreerUtilisateurCommand command) {
        UtilisateurDto dto = new UtilisateurDto(
                UUID.randomUUID(),
                command.nom(),
                command.prenom(),
                command.email(),
                command.role() != null ? command.role() : "CHEF_CHANTIER",
                true,
                command.chantierAttribueId(),
                command.perimetreParc()
        );
        return repositoryPort.save(dto);
    }

    @Override
    @Transactional
    public UtilisateurDto changerStatut(UUID id, boolean actif) {
        UtilisateurDto existant = repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id.toString()));
        UtilisateurDto maj = new UtilisateurDto(
                existant.id(),
                existant.nom(),
                existant.prenom(),
                existant.email(),
                existant.role(),
                actif,
                existant.chantierAttribueId(),
                existant.perimetreParc()
        );
        return repositoryPort.save(maj);
    }
}

