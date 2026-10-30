package sn.cse.suivi.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface AffecterMaterielUseCase {
    void affecter(AffecterMaterielCommand command);

    record AffecterMaterielCommand(
            UUID materielId,
            UUID chantierId,
            UUID besoinId,
            UUID demandeurId,
            LocalDate dateDebut,
            LocalDate dateFinPrevue,
            String motif,
            String affectePar
    ) {}
}
