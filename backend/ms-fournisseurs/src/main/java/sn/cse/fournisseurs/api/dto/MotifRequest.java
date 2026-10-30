package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Motif d'une suspension ou d'une demande de blacklistage.
 *
 * @param motif motif
 */
@Schema(description = "Motif obligatoire")
public record MotifRequest(
    @Schema(
            description = "Motif",
            example = "Retards de livraison répétés sur le chantier CH-THS-01")
        @NotBlank
        @Size(max = 1000)
        String motif) {}
