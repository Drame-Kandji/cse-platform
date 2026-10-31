package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mandatement.
 *
 * @param referenceMandat référence du mandat
 */
@Schema(name = "MandaterRequest", description = "Mandatement d'une facture")
public record MandaterRequest(
    @Schema(example = "MDT-2026-0087") @NotBlank @Size(max = 60) String referenceMandat) {}
