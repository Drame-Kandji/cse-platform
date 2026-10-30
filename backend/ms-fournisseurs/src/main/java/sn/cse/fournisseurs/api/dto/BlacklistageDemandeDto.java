package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Demande de blacklistage en attente (R09).
 *
 * @param demandeurNom demandeur
 * @param motif motif
 * @param date date de la demande
 */
@Schema(description = "Demande de blacklistage en attente de décision de la DG")
public record BlacklistageDemandeDto(
    @Schema(example = "Fatou Sarr") String demandeurNom,
    @Schema(example = "Livraisons non conformes et litiges non résolus") String motif,
    @Schema(example = "2026-10-20T09:15:00Z") Instant date) {}
