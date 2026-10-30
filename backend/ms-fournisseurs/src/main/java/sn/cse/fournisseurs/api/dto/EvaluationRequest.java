package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Évaluation d'un fournisseur (notes 0..10).
 *
 * @param contratId contrat
 * @param qualite qualité
 * @param delais délais
 * @param conformite conformité
 * @param sav SAV
 * @param prix prix
 * @param commentaire commentaire
 */
@Schema(description = "Évaluation d'un fournisseur — chaque critère est noté de 0 à 10")
public record EvaluationRequest(
    @Schema(
            description = "Contrat évalué (optionnel)",
            example = "a1b2c3d4-0000-0000-0000-000000000001")
        UUID contratId,
    @Schema(description = "Qualité du matériel", example = "8") @NotNull @Min(0) @Max(10)
        Integer qualite,
    @Schema(description = "Respect des délais", example = "7") @NotNull @Min(0) @Max(10)
        Integer delais,
    @Schema(description = "Conformité à la commande", example = "9") @NotNull @Min(0) @Max(10)
        Integer conformite,
    @Schema(description = "Service après-vente", example = "6") @NotNull @Min(0) @Max(10)
        Integer sav,
    @Schema(description = "Niveau de prix", example = "7") @NotNull @Min(0) @Max(10) Integer prix,
    @Schema(description = "Commentaire", example = "Livraison conforme, SAV à améliorer")
        @Size(max = 2000)
        String commentaire) {}
