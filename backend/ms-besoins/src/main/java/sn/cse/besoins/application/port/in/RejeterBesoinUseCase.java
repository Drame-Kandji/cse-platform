package sn.cse.besoins.application.port.in;

import java.util.UUID;

public interface RejeterBesoinUseCase {
    void rejeter(UUID besoinId, String motif, UUID rejetePar);
}
