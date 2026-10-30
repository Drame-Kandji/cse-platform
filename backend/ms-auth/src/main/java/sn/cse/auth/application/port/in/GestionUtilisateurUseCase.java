package sn.cse.auth.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GestionUtilisateurUseCase {

    record UtilisateurDto(
            UUID id,
            String nom,
            String prenom,
            String email,
            String role,
            boolean actif,
            UUID chantierAttribueId,
            String perimetreParc
    ) {}

    record CreerUtilisateurCommand(
            String nom,
            String prenom,
            String email,
            String role,
            UUID chantierAttribueId,
            String perimetreParc
    ) {}

    List<UtilisateurDto> listerTous();

    Optional<UtilisateurDto> trouverParId(UUID id);

    UtilisateurDto creer(CreerUtilisateurCommand command);

    UtilisateurDto changerStatut(UUID id, boolean actif);
}

