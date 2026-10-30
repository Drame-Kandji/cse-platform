package sn.cse.besoins.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.besoins.application.port.in.ValiderBesoinUseCase;
import sn.cse.besoins.application.port.out.BesoinRepositoryPort;
import sn.cse.besoins.domain.model.BesoinMateriel;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValiderBesoinService implements ValiderBesoinUseCase {

    private final BesoinRepositoryPort besoinRepositoryPort;

    @Override
    @Transactional
    public void valider(UUID besoinId, UUID validePar) {
        BesoinMateriel besoin = besoinRepositoryPort.findById(besoinId)
                .orElseThrow(() -> new BusinessRuleException("BESOIN_INTROUVABLE", "Besoin introuvable"));
        besoin.valider();
        besoinRepositoryPort.save(besoin);
    }
}
