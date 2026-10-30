package sn.cse.besoins.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.besoins.application.port.in.SoumettreBesoinUseCase;
import sn.cse.besoins.application.port.out.BesoinRepositoryPort;
import sn.cse.besoins.domain.model.BesoinMateriel;

@Service
@RequiredArgsConstructor
public class SoumettreBesoinService implements SoumettreBesoinUseCase {

    private final BesoinRepositoryPort besoinRepositoryPort;

    @Override
    @Transactional
    public SoumissionResult soumettre(SoumettreBesoinCommand command) {
        BesoinMateriel besoin = new BesoinMateriel(
                null,
                command.designation(),
                command.quantite(),
                command.priorite(),
                command.chantierId(),
                command.demandeurId(),
                command.justification()
        );
        BesoinMateriel saved = besoinRepositoryPort.save(besoin);
        return new SoumissionResult(saved.getId(), saved.getReference(), saved.getStatut().name());
    }
}
