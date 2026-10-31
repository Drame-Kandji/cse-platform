package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Réalisation d'un jalon.
 *
 * @param dateReelle date de réalisation
 */
@Schema(name = "JalonRealiseRequest", description = "Date de réalisation d'un jalon")
public record JalonRealiseRequest(@Schema(example = "2026-11-08") @NotNull LocalDate dateReelle) {}
