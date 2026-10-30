package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Libération d'un matériel (corps facultatif).
 *
 * @param dateFinReelle date de fin effective (défaut : aujourd'hui)
 * @param commentaire commentaire
 */
@Schema(name = "LibererRequest", description = "Fin d'affectation et retour au parc")
public record LibererRequest(
    @Schema(example = "2026-10-30", nullable = true) LocalDate dateFinReelle,
    @Schema(example = "Terrassements achevés", nullable = true) @Size(max = 1000)
        String commentaire) {}
