package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import sn.cse.suivi.domain.model.GravitePanne;

/**
 * Signalement d'une panne.
 *
 * @param materielId matériel
 * @param gravite gravité
 * @param description symptômes
 */
@Schema(name = "PanneRequest", description = "Signalement d'une panne")
public record PanneRequest(
    @Schema(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") @NotNull UUID materielId,
    @Schema(example = "MAJEURE") @NotNull GravitePanne gravite,
    @Schema(example = "Fuite hydraulique sur le vérin de flèche") @NotBlank @Size(max = 1000)
        String description) {}
