package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Dépôt d'une offre depuis le portail (le fournisseur est celui du jeton).
 *
 * @param montantPropose montant
 * @param delaiLivraisonJours délai
 * @param garantieMois garantie
 * @param description description
 */
@Schema(name = "OffrePortailRequest", description = "Offre déposée par le fournisseur connecté")
public record OffrePortailRequest(
    @Schema(description = "Montant en FCFA", example = "279000000") @NotNull @Positive
        BigDecimal montantPropose,
    @Schema(example = "40") @NotNull @Min(1) Integer delaiLivraisonJours,
    @Schema(example = "24") @PositiveOrZero Integer garantieMois,
    @Schema(example = "Pelles CAT 320 neuves, garantie constructeur") @Size(max = 4000)
        String description) {}
