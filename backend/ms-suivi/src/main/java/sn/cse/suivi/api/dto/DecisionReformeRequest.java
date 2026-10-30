package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Décision de la Direction Générale sur une demande de réforme (R13, étape 2).
 *
 * @param approuve décision
 * @param commentaire commentaire
 */
@Schema(name = "DecisionReformeRequest", description = "Validation ou refus d'une réforme")
public record DecisionReformeRequest(
    @Schema(example = "true") @NotNull Boolean approuve,
    @Schema(example = "Accord : mise en vente aux enchères", nullable = true) @Size(max = 500)
        String commentaire) {}
