package sn.cse.suivi.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.cse.suivi.application.port.in.VerifierDisponibiliteUseCase;
import sn.cse.suivi.application.port.out.MaterielRepositoryPort;
import sn.cse.suivi.domain.model.EtatMateriel;

@Service
@RequiredArgsConstructor
public class VerifierDisponibiliteService implements VerifierDisponibiliteUseCase {

    private final MaterielRepositoryPort materielRepositoryPort;

    @Override
    public DisponibiliteResult verifier(String categorie, String designation, int quantite) {
        long disponibles = materielRepositoryPort.countByEtatAndCategorie(EtatMateriel.DISPONIBLE, categorie);
        boolean ok = disponibles >= quantite;
        return new DisponibiliteResult(ok, (int) disponibles);
    }
}
