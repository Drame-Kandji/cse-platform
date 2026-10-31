package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Génération manuelle d'un contrat.
 *
 * @param appelOffreId appel d'offres attribué
 */
@Schema(name = "CreerContratRequest", description = "Génération du contrat d'un AO attribué (R03)")
public record CreerContratRequest(
    @Schema(example = "d0000000-0000-0000-0000-000000000001") @NotNull UUID appelOffreId) {}
