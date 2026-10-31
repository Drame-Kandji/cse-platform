package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Confirmation de livraison.
 *
 * @param dateLivraisonReelle date de livraison
 * @param numeroBonLivraison bon de livraison
 * @param observations observations
 */
@Schema(name = "ConfirmerLivraisonRequest", description = "Confirmation de la livraison")
public record ConfirmerLivraisonRequest(
    @Schema(example = "2026-12-02") @NotNull LocalDate dateLivraisonReelle,
    @Schema(example = "BL-SE-2026-0412") @NotBlank @Size(max = 60) String numeroBonLivraison,
    @Schema(example = "Deux pelles réceptionnées en bon état, carnets d'entretien remis")
        @Size(max = 4000)
        String observations) {}
