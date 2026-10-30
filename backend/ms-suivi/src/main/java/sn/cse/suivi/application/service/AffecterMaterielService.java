package sn.cse.suivi.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.suivi.application.port.in.AffecterMaterielUseCase;
import sn.cse.suivi.application.port.out.AffectationRepositoryPort;
import sn.cse.suivi.application.port.out.MaterielRepositoryPort;
import sn.cse.suivi.domain.model.Materiel;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AffecterMaterielService implements AffecterMaterielUseCase {

    private final MaterielRepositoryPort materielRepositoryPort;
    private final AffectationRepositoryPort affectationRepositoryPort;

    @Override
    @Transactional
    public void affecter(AffecterMaterielCommand command) {
        Materiel materiel = materielRepositoryPort.findById(command.materielId())
                .orElseThrow(() -> new BusinessRuleException("MATERIEL_INTROUVABLE", "Matériel introuvable"));

        if (!materiel.estAffectable()) {
            throw new BusinessRuleException("MATERIEL_NON_AFFECTABLE", "Ce matériel n'est pas affectable (état: " + materiel.getEtat() + ")");
        }

        if (affectationRepositoryPort.hasActiveConflict(command.materielId())) {
            throw new BusinessRuleException("CONFLIT_AFFECTATION", "Conflit de dates d'affectation détecté");
        }

        materiel.affecter(command.chantierId());
        materielRepositoryPort.save(materiel);

        affectationRepositoryPort.saveAffectation(
                UUID.randomUUID(),
                command.materielId(),
                command.chantierId(),
                command.besoinId(),
                command.demandeurId(),
                command.dateDebut(),
                command.dateFinPrevue(),
                command.motif(),
                command.affectePar()
        );
    }
}
