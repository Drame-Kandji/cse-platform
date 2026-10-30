package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Demande d'affectation.
 *
 * @param chantierId chantier de destination
 * @param besoinId besoin d'origine
 * @param dateDebut début prévu
 * @param dateFinPrevue fin prévue
 */
@Schema(name = "AffecterRequest", description = "Affectation d'un matériel à un chantier")
public record AffecterRequest(
    @Schema(example = "c0000000-0000-0000-0000-000000000002") @NotNull UUID chantierId,
    @Schema(example = "b0000000-0000-0000-0000-000000000004", nullable = true) UUID besoinId,
    @Schema(example = "2026-11-02") @NotNull LocalDate dateDebut,
    @Schema(example = "2027-02-28") @NotNull LocalDate dateFinPrevue) {}
