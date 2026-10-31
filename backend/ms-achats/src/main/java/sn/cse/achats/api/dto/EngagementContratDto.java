package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import sn.cse.achats.domain.model.StatutApprobation;

/**
 * Engagement financier rattaché au contrat.
 *
 * @param statut décision DAF
 * @param montant montant engagé
 * @param parNom décideur
 * @param dateDemande demande
 * @param dateDecision décision
 * @param commentaire commentaire
 */
@Schema(name = "EngagementContrat", description = "Engagement financier demandé à la DAF (R06)")
public record EngagementContratDto(
    @Schema(example = "EN_ATTENTE") StatutApprobation statut,
    @Schema(example = "285000000") BigDecimal montant,
    @Schema(example = "Ousmane Ba") String parNom,
    @Schema(example = "2026-10-15T09:00:00Z") Instant dateDemande,
    @Schema(example = "2026-10-16T11:00:00Z") Instant dateDecision,
    @Schema(example = "Crédits disponibles sur la ligne investissements 2026")
        String commentaire) {}
