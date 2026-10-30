package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Relevé du compteur horaire.
 *
 * @param heuresFonctionnement nouvelle valeur (≥ compteur actuel)
 */
@Schema(name = "ReleveHeuresRequest", description = "Relevé du compteur horaire")
public record ReleveHeuresRequest(
    @Schema(example = "4870") @NotNull @PositiveOrZero Long heuresFonctionnement) {}
