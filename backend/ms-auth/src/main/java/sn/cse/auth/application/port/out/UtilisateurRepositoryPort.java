package sn.cse.auth.application.port.out;

import sn.cse.auth.application.port.in.GestionUtilisateurUseCase.UtilisateurDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepositoryPort {
    List<UtilisateurDto> findAll();
    Optional<UtilisateurDto> findById(UUID id);
    UtilisateurDto save(UtilisateurDto utilisateur);
}

