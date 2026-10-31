package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Proposition d'attribution.
 *
 * @param offreId offre proposée
 * @param justification justification
 */
@Schema(name = "AttribuerRequest", description = "Proposition d'attribution par le RA (R11)")
public record AttribuerRequest(
    @Schema(example = "6b1f6c1e-2a41-4d63-9a59-1f3e9b7f0c11") @NotNull UUID offreId,
    @Schema(example = "Meilleur score global et délai compatible avec le planning du chantier.")
        @NotBlank
        @Size(max = 2000)
        String justification) {}
