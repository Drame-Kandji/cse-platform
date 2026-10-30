package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.domain.model.TypeMaintenance;

/**
 * Ordre de travail de maintenance.
 *
 * @param id identifiant
 * @param numeroOt numéro
 * @param materielId matériel
 * @param materielCode code du matériel
 * @param materielDesignation désignation du matériel
 * @param chantierId chantier
 * @param chantierNom nom du chantier
 * @param type type
 * @param statut statut
 * @param description travaux
 * @param datePlanifiee date prévue
 * @param dateDebut début effectif
 * @param dateFin fin effective
 * @param technicien intervenant
 * @param cout coût (FCFA)
 * @param heuresCompteur compteur relevé
 * @param observations compte rendu
 * @param panneId panne à l'origine
 */
@Schema(name = "Maintenance", description = "Ordre de travail de maintenance")
public record MaintenanceDto(
    @Schema(example = "3e4f5a6b-7c8d-4e9f-0a1b-2c3d4e5f6a7b") UUID id,
    @Schema(example = "OT-2026-0042") String numeroOt,
    @Schema(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") UUID materielId,
    @Schema(example = "PEL-CAT320-01") String materielCode,
    @Schema(example = "Pelle hydraulique sur chenilles 20 t") String materielDesignation,
    @Schema(example = "c0000000-0000-0000-0000-000000000001", nullable = true) UUID chantierId,
    @Schema(example = "Extension autoroute Dakar–AIBD", nullable = true) String chantierNom,
    @Schema(example = "PREVENTIVE") TypeMaintenance type,
    @Schema(example = "PLANIFIEE") StatutMaintenance statut,
    @Schema(example = "Vidange moteur, remplacement des filtres à huile et à gasoil")
        String description,
    @Schema(example = "2026-11-05") LocalDate datePlanifiee,
    @Schema(example = "2026-11-05", nullable = true) LocalDate dateDebut,
    @Schema(example = "2026-11-06", nullable = true) LocalDate dateFin,
    @Schema(example = "Atelier central — Moustapha Gueye", nullable = true) String technicien,
    @Schema(description = "Coût en FCFA", example = "685000", nullable = true) BigDecimal cout,
    @Schema(example = "4830", nullable = true) Long heuresCompteur,
    @Schema(example = "RAS, prochaine vidange à 5330 h", nullable = true) String observations,
    @Schema(example = "5a6b7c8d-9e0f-4a1b-2c3d-4e5f6a7b8c9d", nullable = true) UUID panneId) {}
