package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Clôture d'un ordre de travail.
 *
 * @param cout coût total (FCFA)
 * @param observations compte rendu
 * @param heuresCompteur compteur relevé
 */
@Schema(name = "ClotureMaintenanceRequest", description = "Clôture d'un ordre de travail")
public record ClotureMaintenanceRequest(
    @Schema(description = "Coût total en FCFA", example = "685000") @NotNull @PositiveOrZero
        BigDecimal cout,
    @Schema(example = "Filtres remplacés, niveaux faits", nullable = true) @Size(max = 1000)
        String observations,
    @Schema(example = "4830", nullable = true) @PositiveOrZero Long heuresCompteur) {}
