package sn.cse.suivi.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ChantierMaintenancePort {

    record ChantierDto(
            UUID id,
            String nom,
            String adresse,
            String ville,
            BigDecimal latitude,
            BigDecimal longitude,
            String statut,
            BigDecimal budget
    ) {}

    List<ChantierDto> findAllChantiers();
    ChantierDto saveChantier(ChantierDto chantier);
}

