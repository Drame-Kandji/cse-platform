package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;

/**
 * Position d'un fournisseur dans le classement.
 *
 * @param rang rang
 * @param fournisseurId fournisseur
 * @param raisonSociale raison sociale
 * @param scorePerformance score
 * @param nombreEvaluations évaluations
 * @param tauxLivraisonALHeure ponctualité
 * @param nombreLitiges litiges (tous statuts)
 * @param specialites spécialités
 */
@Schema(description = "Classement dynamique des fournisseurs actifs")
public record ClassementDto(
    @Schema(example = "1") int rang,
    @Schema(example = "f0000000-0000-0000-0000-000000000001") UUID fournisseurId,
    @Schema(example = "Sahel Engins SA") String raisonSociale,
    @Schema(example = "84.3") BigDecimal scorePerformance,
    @Schema(example = "6") int nombreEvaluations,
    @Schema(example = "90.0") BigDecimal tauxLivraisonALHeure,
    @Schema(example = "1") int nombreLitiges,
    @Schema(example = "[\"MATERIEL_ROULANT\"]") List<CategorieMateriel> specialites) {}
