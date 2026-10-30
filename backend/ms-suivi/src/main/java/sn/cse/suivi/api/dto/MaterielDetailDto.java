package sn.cse.suivi.api.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Fiche complète : champs du matériel (à plat) et 20 derniers éléments de chaque historique.
 *
 * @param materiel matériel (champs aplatis)
 * @param affectations dernières affectations
 * @param maintenances derniers ordres de travail
 * @param pannes dernières pannes
 * @param mouvements derniers mouvements
 */
@Schema(name = "MaterielDetail", description = "Matériel et historiques récents")
public record MaterielDetailDto(
    @JsonUnwrapped MaterielDto materiel,
    @Schema(description = "Dernières affectations") List<AffectationDto> affectations,
    @Schema(description = "Derniers ordres de travail") List<MaintenanceDto> maintenances,
    @Schema(description = "Dernières pannes") List<PanneDto> pannes,
    @Schema(description = "Derniers mouvements") List<MouvementDto> mouvements) {}
