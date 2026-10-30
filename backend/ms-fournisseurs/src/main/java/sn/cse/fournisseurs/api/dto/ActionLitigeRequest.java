package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.cse.fournisseurs.domain.model.TypeActionLitige;

/**
 * Action sur un litige.
 *
 * @param type type d'action
 * @param commentaire commentaire
 */
@Schema(description = "Action de suivi, résolution ou classement d'un litige")
public record ActionLitigeRequest(
    @Schema(
            description = "Type d'action (OUVERTURE interdite)",
            example = "RESOLUTION",
            allowableValues = {"ACTION", "RESOLUTION", "CLASSEMENT"})
        @NotNull
        TypeActionLitige type,
    @Schema(example = "Avoir de 3 500 000 FCFA accepté par le fournisseur")
        @NotBlank
        @Size(max = 2000)
        String commentaire) {}
