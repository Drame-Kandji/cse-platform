package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import sn.cse.suivi.domain.model.CategorieMateriel;

/**
 * Type d'engin du référentiel.
 *
 * @param id identifiant
 * @param code code
 * @param libelle libellé
 * @param categorie catégorie
 * @param seuilMaintenanceHeures seuil horaire
 * @param seuilMaintenanceJours seuil calendaire
 * @param actif proposé à la saisie
 */
@Schema(name = "TypeEngin", description = "Type d'engin et seuils de maintenance préventive")
public record TypeEnginDto(
    @Schema(example = "e0000000-0000-0000-0000-000000000001") UUID id,
    @Schema(example = "PELLE_HYD") String code,
    @Schema(example = "Pelle hydraulique") String libelle,
    @Schema(example = "MATERIEL_ROULANT") CategorieMateriel categorie,
    @Schema(example = "500", nullable = true) Integer seuilMaintenanceHeures,
    @Schema(example = "180", nullable = true) Integer seuilMaintenanceJours,
    @Schema(example = "true") boolean actif) {}
