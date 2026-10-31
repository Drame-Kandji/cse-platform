package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Décision d'approbation.
 *
 * @param approuve décision
 * @param commentaire commentaire
 */
@Schema(name = "DecisionRequest", description = "Décision d'un approbateur")
public record DecisionRequest(
    @Schema(example = "true") @NotNull Boolean approuve,
    @Schema(example = "Conforme au besoin du chantier.") @Size(max = 2000) String commentaire) {}
