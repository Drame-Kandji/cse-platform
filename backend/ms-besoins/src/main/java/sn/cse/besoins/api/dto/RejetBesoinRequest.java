package sn.cse.besoins.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code PATCH /api/besoins/{id}/rejeter}.
 *
 * @param motif motif du rejet (obligatoire, R08 : 422 {@code MOTIF_REJET_OBLIGATOIRE} si absent)
 */
@Schema(name = "RejetBesoin", description = "Rejet motivé d'un besoin")
public record RejetBesoinRequest(
    @Schema(
            description = "Motif du rejet (obligatoire, règle R08)",
            example = "Doublon du besoin BES-2026-0012 déjà validé pour ce chantier",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @Size(max = 1000, message = "1000 caractères au maximum")
        String motif) {}
