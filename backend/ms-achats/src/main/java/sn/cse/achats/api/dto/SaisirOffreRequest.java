package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saisie d'une offre reçue hors portail.
 *
 * @param fournisseurId fournisseur
 * @param montantPropose montant
 * @param delaiLivraisonJours délai
 * @param garantieMois garantie
 * @param description description
 */
@Schema(name = "SaisirOffreRequest", description = "Offre reçue hors portail, saisie par le RA")
public record SaisirOffreRequest(
    @Schema(example = "f0000000-0000-0000-0000-000000000001") @NotNull UUID fournisseurId,
    @Schema(description = "Montant en FCFA", example = "285000000") @NotNull @Positive
        BigDecimal montantPropose,
    @Schema(example = "45") @NotNull @Min(1) Integer delaiLivraisonJours,
    @Schema(example = "24") @PositiveOrZero Integer garantieMois,
    @Schema(example = "Deux pelles Komatsu PC210, formation des conducteurs incluse")
        @Size(max = 4000)
        String description) {}
