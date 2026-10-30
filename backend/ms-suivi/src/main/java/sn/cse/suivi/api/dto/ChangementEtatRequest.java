package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.cse.suivi.domain.model.EtatMateriel;

/**
 * Changement d'état manuel.
 *
 * @param etat {@code DISPONIBLE} (remise en service) ou {@code EN_MAINTENANCE}
 * @param motif motif
 */
@Schema(name = "ChangementEtatRequest", description = "Changement d'état manuel d'un matériel")
public record ChangementEtatRequest(
    @Schema(
            example = "EN_MAINTENANCE",
            allowableValues = {"DISPONIBLE", "EN_MAINTENANCE"})
        @NotNull
        EtatMateriel etat,
    @Schema(example = "Immobilisation pour contrôle réglementaire") @NotBlank @Size(max = 500)
        String motif) {}
