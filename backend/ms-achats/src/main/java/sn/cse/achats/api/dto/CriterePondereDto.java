package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import sn.cse.achats.domain.model.CritereEvaluation;

/**
 * Critère d'évaluation pondéré.
 *
 * @param code critère
 * @param poids poids sur 100
 */
@Schema(
    name = "CriterePondere",
    description = "Critère d'évaluation et son poids (somme des poids = 100)")
public record CriterePondereDto(
    @Schema(description = "Critère", example = "PRIX") @NotNull CritereEvaluation code,
    @Schema(description = "Poids en points sur 100", example = "40") @NotNull @Min(1) @Max(100)
        Integer poids) {}
