package sn.cse.besoins.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Corps facultatif de {@code POST /api/besoins/{id}/orienter}.
 *
 * @param materielIds matériels à affecter en priorité
 */
@Schema(name = "OrientationBesoin", description = "Relance de l'orientation d'un besoin validé")
public record OrientationBesoinRequest(
    @ArraySchema(
            arraySchema =
                @Schema(description = "Matériels à affecter en priorité", nullable = true),
            schema = @Schema(example = "a1000000-0000-0000-0000-000000000002"))
        @Size(max = 100, message = "100 matériels au maximum")
        List<UUID> materielIds) {}
