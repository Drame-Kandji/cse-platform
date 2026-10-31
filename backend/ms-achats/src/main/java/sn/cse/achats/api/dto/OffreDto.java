package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import sn.cse.achats.domain.model.CritereEvaluation;

/**
 * Offre d'un fournisseur (anonymisée tant que l'appel d'offres est publié, R10).
 *
 * @param id identifiant
 * @param appelOffreId appel d'offres
 * @param codeAnonyme code opaque
 * @param fournisseurId fournisseur (null tant que PUBLIE)
 * @param fournisseurNom fournisseur (null tant que PUBLIE)
 * @param montantPropose montant
 * @param delaiLivraisonJours délai
 * @param garantieMois garantie
 * @param description description
 * @param dateDepot dépôt
 * @param notes notes 0..100 par critère
 * @param scoreGlobal score pondéré
 * @param rang rang
 * @param retenue offre retenue
 */
@Schema(
    name = "Offre",
    description = "Offre (identité masquée tant que l'appel d'offres est publié, R10)")
public record OffreDto(
    @Schema(example = "6b1f6c1e-2a41-4d63-9a59-1f3e9b7f0c11") UUID id,
    @Schema(example = "d0000000-0000-0000-0000-000000000001") UUID appelOffreId,
    @Schema(example = "OFR-7F3A2C") String codeAnonyme,
    @Schema(
            description = "Null tant que l'AO est PUBLIE (R10)",
            example = "f0000000-0000-0000-0000-000000000001")
        UUID fournisseurId,
    @Schema(description = "Null tant que l'AO est PUBLIE (R10)", example = "Sahel Engins SA")
        String fournisseurNom,
    @Schema(description = "Montant en FCFA", example = "285000000") BigDecimal montantPropose,
    @Schema(example = "45") int delaiLivraisonJours,
    @Schema(example = "24") Integer garantieMois,
    @Schema(example = "Deux pelles Komatsu PC210, formation des conducteurs incluse")
        String description,
    @Schema(example = "2026-10-08T14:20:00Z") Instant dateDepot,
    @Schema(
            description = "Notes 0..100 (PRIX et DELAI calculés)",
            example = "{\"PRIX\": 100.00, \"DELAI\": 88.89, \"QUALITE_TECHNIQUE\": 80.00}")
        Map<CritereEvaluation, BigDecimal> notes,
    @Schema(
            description = "Score pondéré (null tant que tous les critères ne sont pas notés)",
            example = "89.33")
        BigDecimal scoreGlobal,
    @Schema(example = "1") Integer rang,
    @Schema(example = "false") boolean retenue) {}
