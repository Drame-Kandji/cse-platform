package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import sn.cse.fournisseurs.domain.model.GraviteLitige;

/**
 * Ouverture d'un litige.
 *
 * @param contratId contrat
 * @param objet objet
 * @param description description
 * @param gravite gravité
 */
@Schema(description = "Ouverture d'un litige fournisseur")
public record LitigeRequest(
    @Schema(
            description = "Contrat concerné (optionnel)",
            example = "a1b2c3d4-0000-0000-0000-000000000001")
        UUID contratId,
    @Schema(example = "Livraison incomplète de camions-bennes") @NotBlank @Size(max = 255)
        String objet,
    @Schema(example = "2 camions sur 5 livrés au chantier CH-THS-01 à la date prévue")
        @NotBlank
        @Size(max = 4000)
        String description,
    @Schema(example = "MOYENNE") @NotNull GraviteLitige gravite) {}
