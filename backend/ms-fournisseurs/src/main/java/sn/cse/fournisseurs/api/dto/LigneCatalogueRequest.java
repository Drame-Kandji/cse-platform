package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;

/**
 * Ligne de catalogue à publier (upsert par référence).
 *
 * @param reference référence
 * @param designation désignation
 * @param categorie catégorie
 * @param prixUnitaire prix unitaire FCFA
 * @param unite unité
 * @param delaiLivraisonJours délai
 */
@Schema(
    description =
        "Ligne de catalogue (upsert par référence ; tout changement de prix est historisé)")
public record LigneCatalogueRequest(
    @Schema(example = "PH-CAT320") @NotBlank @Size(max = 80) String reference,
    @Schema(example = "Pelle hydraulique sur chenilles 20 t") @NotBlank @Size(max = 255)
        String designation,
    @Schema(example = "MATERIEL_ROULANT") @NotNull CategorieMateriel categorie,
    @Schema(description = "Prix unitaire en FCFA", example = "98500000")
        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 0)
        BigDecimal prixUnitaire,
    @Schema(example = "unité") @NotBlank @Size(max = 30) String unite,
    @Schema(example = "45") @NotNull @Min(0) Integer delaiLivraisonJours) {}
