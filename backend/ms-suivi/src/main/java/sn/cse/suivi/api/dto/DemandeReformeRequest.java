package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Demande de réforme (R13, étape 1).
 *
 * @param motif justification
 */
@Schema(name = "DemandeReformeRequest", description = "Demande de réforme d'un matériel")
public record DemandeReformeRequest(
    @Schema(example = "Châssis fissuré, réparation estimée à 60 % de la valeur à neuf")
        @NotBlank
        @Size(max = 500)
        String motif) {}
