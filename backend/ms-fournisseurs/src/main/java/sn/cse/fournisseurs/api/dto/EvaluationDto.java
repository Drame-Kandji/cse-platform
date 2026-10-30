package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Évaluation.
 *
 * @param id identifiant
 * @param contratId contrat
 * @param qualite qualité
 * @param delais délais
 * @param conformite conformité
 * @param sav SAV
 * @param prix prix
 * @param noteGlobale note globale 0..100
 * @param commentaire commentaire
 * @param evaluateurNom évaluateur
 * @param date date
 */
@Schema(description = "Évaluation d'un fournisseur")
public record EvaluationDto(
    @Schema(example = "e0000000-0000-0000-0000-000000000001") UUID id,
    @Schema(example = "a1b2c3d4-0000-0000-0000-000000000001") UUID contratId,
    @Schema(example = "8") int qualite,
    @Schema(example = "7") int delais,
    @Schema(example = "9") int conformite,
    @Schema(example = "6") int sav,
    @Schema(example = "7") int prix,
    @Schema(description = "Note globale pondérée 0..100", example = "76.0") BigDecimal noteGlobale,
    @Schema(example = "Livraison conforme, SAV à améliorer") String commentaire,
    @Schema(example = "Ibrahima Fall") String evaluateurNom,
    @Schema(example = "2026-10-12T14:30:00Z") Instant date) {}
