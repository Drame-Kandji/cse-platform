package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.StatutAffectation;

/**
 * Affectation d'un matériel à un chantier.
 *
 * @param id identifiant
 * @param materielId matériel
 * @param materielCode code du matériel
 * @param materielDesignation désignation du matériel
 * @param categorie catégorie du matériel
 * @param chantierId chantier
 * @param chantierNom nom du chantier
 * @param besoinId besoin d'origine
 * @param dateDebut début
 * @param dateFinPrevue fin prévue
 * @param dateFinReelle fin réelle
 * @param statut statut
 * @param dateReception instant de réception
 * @param observationsReception observations de réception
 * @param fournisseurId fournisseur (matériel livré)
 * @param contratId contrat (matériel livré)
 */
@Schema(name = "Affectation", description = "Affectation d'un matériel à un chantier")
public record AffectationDto(
    @Schema(example = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d") UUID id,
    @Schema(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") UUID materielId,
    @Schema(example = "PEL-CAT320-01") String materielCode,
    @Schema(example = "Pelle hydraulique sur chenilles 20 t") String materielDesignation,
    @Schema(example = "MATERIEL_ROULANT") CategorieMateriel categorie,
    @Schema(example = "c0000000-0000-0000-0000-000000000001") UUID chantierId,
    @Schema(example = "Extension autoroute Dakar–AIBD") String chantierNom,
    @Schema(example = "b0000000-0000-0000-0000-000000000004", nullable = true) UUID besoinId,
    @Schema(example = "2026-11-02") LocalDate dateDebut,
    @Schema(example = "2027-02-28") LocalDate dateFinPrevue,
    @Schema(example = "2027-02-20", nullable = true) LocalDate dateFinReelle,
    @Schema(example = "EN_ATTENTE_RECEPTION") StatutAffectation statut,
    @Schema(example = "2026-11-03T08:40:00Z", nullable = true) Instant dateReception,
    @Schema(example = "Reçu en bon état, plein effectué", nullable = true)
        String observationsReception,
    @Schema(example = "f0000000-0000-0000-0000-000000000001", nullable = true) UUID fournisseurId,
    @Schema(example = "4d2a6e8f-1b3c-4e5a-9f7d-2c4b6a8e0f1d", nullable = true) UUID contratId) {}
