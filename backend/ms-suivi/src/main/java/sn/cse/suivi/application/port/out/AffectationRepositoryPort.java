package sn.cse.suivi.application.port.out;

import java.util.UUID;

public interface AffectationRepositoryPort {
    boolean hasActiveConflict(UUID materielId);
    void saveAffectation(UUID id, UUID materielId, UUID chantierId, UUID besoinId, UUID demandeurId,
                          java.time.LocalDate dateDebut, java.time.LocalDate dateFinPrevue, String motif, String affectePar);
}
