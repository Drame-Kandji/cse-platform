package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import sn.cse.suivi.domain.model.GravitePanne;
import sn.cse.suivi.domain.model.StatutPanne;

/**
 * Panne signalée.
 *
 * @param id identifiant
 * @param materielId matériel
 * @param materielCode code du matériel
 * @param materielDesignation désignation du matériel
 * @param chantierId chantier
 * @param chantierNom nom du chantier
 * @param gravite gravité
 * @param description symptômes
 * @param statut statut
 * @param signaleParId auteur
 * @param signaleParNom nom de l'auteur
 * @param dateSignalement instant du signalement
 * @param maintenanceId ordre de travail de prise en charge
 */
@Schema(name = "Panne", description = "Panne signalée sur un matériel")
public record PanneDto(
    @Schema(example = "5a6b7c8d-9e0f-4a1b-2c3d-4e5f6a7b8c9d") UUID id,
    @Schema(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") UUID materielId,
    @Schema(example = "PEL-CAT320-01") String materielCode,
    @Schema(example = "Pelle hydraulique sur chenilles 20 t") String materielDesignation,
    @Schema(example = "c0000000-0000-0000-0000-000000000001", nullable = true) UUID chantierId,
    @Schema(example = "Extension autoroute Dakar-AIBD", nullable = true) String chantierNom,
    @Schema(example = "MAJEURE") GravitePanne gravite,
    @Schema(example = "Fuite hydraulique sur le vérin de flèche") String description,
    @Schema(example = "SIGNALEE") StatutPanne statut,
    @Schema(example = "00000000-0000-0000-0000-000000000101", nullable = true) UUID signaleParId,
    @Schema(example = "Mamadou Diop", nullable = true) String signaleParNom,
    @Schema(example = "2026-10-29T15:20:00Z") Instant dateSignalement,
    @Schema(example = "3e4f5a6b-7c8d-4e9f-0a1b-2c3d4e5f6a7b", nullable = true)
        UUID maintenanceId) {}
