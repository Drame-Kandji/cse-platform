package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Décision de la Direction Générale sur une demande de blacklistage (R09, étape 2).
 *
 * @param approuve approbation
 * @param commentaire commentaire
 */
@Schema(description = "Décision de la DG sur la demande de blacklistage")
public record DecisionBlacklistageRequest(
    @Schema(description = "true = blacklister, false = refuser la demande", example = "true")
        @NotNull
        Boolean approuve,
    @Schema(description = "Commentaire", example = "Manquements graves confirmés par l'audit")
        @Size(max = 1000)
        String commentaire) {}
