package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Refus de signature du fournisseur.
 *
 * @param motif motif du refus (obligatoire)
 */
@Schema(name = "RefuserSignatureRequest", description = "Refus de signature d'un contrat")
public record RefuserSignatureRequest(
    @Schema(
            example =
                "Le taux de pénalité de 0,1 % par jour dépasse nos conditions générales ; nous"
                    + " proposons 0,05 %.")
        @NotBlank
        @Size(max = 2000)
        String motif) {}
