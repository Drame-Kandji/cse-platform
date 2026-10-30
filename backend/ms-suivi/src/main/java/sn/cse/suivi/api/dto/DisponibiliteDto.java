package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Résultat d'une demande de disponibilité.
 *
 * @param disponible quantité suffisante
 * @param quantiteDemandee quantité demandée
 * @param quantiteDisponible quantité disponible
 * @param materiels matériels proposés
 */
@Schema(name = "Disponibilite", description = "Disponibilité de matériels")
public record DisponibiliteDto(
    @Schema(example = "true") boolean disponible,
    @Schema(example = "2") int quantiteDemandee,
    @Schema(example = "3") long quantiteDisponible,
    @Schema(description = "Matériels disponibles proposés") List<MaterielDto> materiels) {}
