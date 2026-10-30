package sn.cse.besoins.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.besoins.application.port.in.RejeterBesoinUseCase;
import sn.cse.besoins.application.port.out.BesoinRepositoryPort;
import sn.cse.besoins.domain.model.BesoinMateriel;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RejeterBesoinService implements RejeterBesoinUseCase {

    private final BesoinRepositoryPort besoinRepositoryPort;

    @Override
    @Transactional
    public void rejeter(UUID besoinId, String motif, UUID rejetePar) {
        BesoinMateriel besoin = besoinRepositoryPort.findById(besoinId)
                .orElseThrow(() -> new BusinessRuleException("BESOIN_INTROUVABLE", "Besoin introuvable"));
        besoin.rejeter(motif);
        besoinRepositoryPort.save(besoin);
    }
}
