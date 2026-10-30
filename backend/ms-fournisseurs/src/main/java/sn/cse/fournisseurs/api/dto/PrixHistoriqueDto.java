package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entrée d'historique de prix.
 *
 * @param reference référence
 * @param designation désignation
 * @param prixUnitaire prix
 * @param date date du changement
 */
@Schema(description = "Historique de prix d'une référence")
public record PrixHistoriqueDto(
    @Schema(example = "PH-CAT320") String reference,
    @Schema(example = "Pelle hydraulique sur chenilles 20 t") String designation,
    @Schema(example = "96000000") BigDecimal prixUnitaire,
    @Schema(example = "2026-03-15T08:00:00Z") Instant date) {}
