package sn.cse.besoins.application.port.in;

import java.util.UUID;

public interface ValiderBesoinUseCase {
    void valider(UUID besoinId, UUID validePar);
}
