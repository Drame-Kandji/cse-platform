package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import sn.cse.achats.domain.model.StatutApprobation;

/**
 * Engagement (vue DAF).
 *
 * @param contratId contrat
 * @param contratNumero numéro
 * @param fournisseurNom titulaire
 * @param chantierNom chantier
 * @param objet objet
 * @param montant montant
 * @param statut statut
 * @param dateDemande demande
 * @param dateDecision décision
 * @param parNom décideur
 */
@Schema(name = "Engagement", description = "Engagement financier à valider par la DAF")
public record EngagementDto(
    @Schema(example = "a0000000-0000-0000-0000-000000000001") UUID contratId,
    @Schema(example = "CTR-2026-001") String contratNumero,
    @Schema(example = "Sahel Engins SA") String fournisseurNom,
    @Schema(example = "Extension autoroute Dakar-AIBD") String chantierNom,
    @Schema(example = "Acquisition de deux pelles hydrauliques 20 t") String objet,
    @Schema(example = "285000000") BigDecimal montant,
    @Schema(example = "EN_ATTENTE") StatutApprobation statut,
    @Schema(example = "2026-10-15T09:00:00Z") Instant dateDemande,
    @Schema(example = "2026-10-16T11:00:00Z") Instant dateDecision,
    @Schema(example = "Ousmane Ba") String parNom) {}
