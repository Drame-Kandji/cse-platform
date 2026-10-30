package sn.cse.besoins.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Corps de {@code PATCH /api/besoins/{id}/valider} (facultatif).
 *
 * @param commentaire commentaire du validateur
 * @param materielIds matériels à affecter en priorité
 */
@Schema(
    name = "ValidationBesoin",
    description = "Validation d'un besoin par le Responsable Matériel")
public record ValidationBesoinRequest(
    @Schema(
            description = "Commentaire du validateur",
            example = "Validé : priorité au lot terrassement",
            nullable = true)
        @Size(max = 2000, message = "2000 caractères au maximum")
        String commentaire,
    @ArraySchema(
            arraySchema =
                @Schema(
                    description =
                        "Matériels à affecter en priorité (complétés si besoin par d'autres"
                            + " matériels disponibles)",
                    nullable = true),
            schema = @Schema(example = "a1000000-0000-0000-0000-000000000001"))
        @Size(max = 100, message = "100 matériels au maximum")
        List<UUID> materielIds) {}
