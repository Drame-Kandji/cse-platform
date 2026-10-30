package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Création / modification d'un chantier.
 *
 * @param code code unique
 * @param nom nom
 * @param localisation localisation
 * @param region région
 * @param latitude latitude
 * @param longitude longitude
 * @param budget budget (FCFA)
 * @param montantMarche montant du marché (FCFA)
 * @param dateDebut date de début
 * @param dateFinPrevue fin prévue
 * @param chefChantierId chef de chantier
 * @param chefChantierNom nom du chef
 * @param version version lue (obligatoire en modification)
 */
@Schema(name = "ChantierRequest", description = "Données d'un chantier")
public record ChantierRequest(
    @Schema(example = "CH-DKR-02") @NotBlank @Size(max = 40) String code,
    @Schema(example = "Échangeur de Keur Massar") @NotBlank @Size(max = 200) String nom,
    @Schema(example = "Keur Massar, Dakar") @NotBlank @Size(max = 200) String localisation,
    @Schema(example = "Dakar") @NotBlank @Size(max = 100) String region,
    @Schema(example = "14.7833") @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @Schema(example = "-17.3167") @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
    @Schema(description = "Budget en FCFA", example = "4500000000") @NotNull @PositiveOrZero
        BigDecimal budget,
    @Schema(description = "Montant du marché en FCFA", example = "5200000000")
        @NotNull
        @PositiveOrZero
        BigDecimal montantMarche,
    @Schema(example = "2026-11-02") @NotNull LocalDate dateDebut,
    @Schema(example = "2027-06-30", nullable = true) LocalDate dateFinPrevue,
    @Schema(example = "00000000-0000-0000-0000-000000000101", nullable = true) UUID chefChantierId,
    @Schema(example = "Mamadou Diop", nullable = true) @Size(max = 150) String chefChantierNom,
    @Schema(
            description = "Version lue (obligatoire en modification)",
            example = "3",
            nullable = true)
        Long version) {}
