package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Signature d'une partie au contrat.
 *
 * @param parNom signataire
 * @param date instant de la signature
 */
@Schema(name = "SignatureContrat", description = "Signature du contrat par l'une des parties")
public record SignatureDto(
    @Schema(example = "Fatou Sarr") String parNom,
    @Schema(example = "2026-10-15T10:00:00Z") Instant date) {}
