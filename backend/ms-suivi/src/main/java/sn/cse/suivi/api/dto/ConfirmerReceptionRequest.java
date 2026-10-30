package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Confirmation de réception (corps facultatif).
 *
 * @param affectationId affectation visée (défaut : l'affectation active du matériel)
 * @param observations observations
 */
@Schema(name = "ConfirmerReceptionRequest", description = "Confirmation de réception sur chantier")
public record ConfirmerReceptionRequest(
    @Schema(example = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d", nullable = true) UUID affectationId,
    @Schema(example = "Reçu en bon état, plein effectué", nullable = true) @Size(max = 1000)
        String observations) {}
