package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import sn.cse.suivi.domain.model.CategorieMateriel;

/**
 * Création / modification d'un type d'engin.
 *
 * @param code code unique
 * @param libelle libellé
 * @param categorie catégorie
 * @param seuilMaintenanceHeures seuil horaire
 * @param seuilMaintenanceJours seuil calendaire
 * @param actif proposé à la saisie
 */
@Schema(name = "TypeEnginRequest", description = "Données d'un type d'engin")
public record TypeEnginRequest(
    @Schema(example = "NIVELEUSE") @NotBlank @Size(max = 40) String code,
    @Schema(example = "Niveleuse") @NotBlank @Size(max = 150) String libelle,
    @Schema(example = "MATERIEL_ROULANT") @NotNull CategorieMateriel categorie,
    @Schema(example = "500", nullable = true) @Positive Integer seuilMaintenanceHeures,
    @Schema(example = "180", nullable = true) @Positive Integer seuilMaintenanceJours,
    @Schema(example = "true") @NotNull Boolean actif) {}
