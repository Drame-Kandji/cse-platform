package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.suivi.domain.model.TypeMaintenance;

/**
 * Planification d'un ordre de travail.
 *
 * @param materielId matériel
 * @param type type
 * @param description travaux
 * @param datePlanifiee date prévue
 * @param technicien intervenant
 * @param panneId panne à l'origine
 */
@Schema(name = "MaintenanceRequest", description = "Planification d'un ordre de travail")
public record MaintenanceRequest(
    @Schema(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") @NotNull UUID materielId,
    @Schema(example = "PREVENTIVE") @NotNull TypeMaintenance type,
    @Schema(example = "Vidange moteur, remplacement des filtres à huile et à gasoil")
        @NotBlank
        @Size(max = 1000)
        String description,
    @Schema(example = "2026-11-05") @NotNull LocalDate datePlanifiee,
    @Schema(example = "Atelier central · Moustapha Gueye", nullable = true) @Size(max = 150)
        String technicien,
    @Schema(example = "5a6b7c8d-9e0f-4a1b-2c3d-4e5f6a7b8c9d", nullable = true) UUID panneId) {}
