package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Rejet.
 *
 * @param motif motif
 */
@Schema(name = "RejeterRequest", description = "Rejet d'une facture")
public record RejeterRequest(
    @Schema(example = "Quantités facturées non conformes au bon de livraison")
        @NotBlank
        @Size(max = 2000)
        String motif) {}
