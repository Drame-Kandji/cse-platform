package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Enregistrement d'une facture.
 *
 * @param contratId contrat
 * @param numero numéro de facture du fournisseur
 * @param montantHt hors taxes
 * @param tva montant de TVA
 * @param dateEmission émission
 * @param dateEcheance échéance
 */
@Schema(name = "CreerFactureRequest", description = "Facture reçue d'un fournisseur")
public record CreerFactureRequest(
    @Schema(example = "a0000000-0000-0000-0000-000000000001") @NotNull UUID contratId,
    @Schema(example = "FA-SE-2026-118") @NotBlank @Size(max = 60) String numero,
    @Schema(description = "Montant HT en FCFA", example = "120000000") @NotNull @Positive
        BigDecimal montantHt,
    @Schema(description = "Montant de TVA en FCFA", example = "21600000") @NotNull @PositiveOrZero
        BigDecimal tva,
    @Schema(example = "2026-10-20") @NotNull LocalDate dateEmission,
    @Schema(example = "2026-11-19") @NotNull LocalDate dateEcheance) {}
