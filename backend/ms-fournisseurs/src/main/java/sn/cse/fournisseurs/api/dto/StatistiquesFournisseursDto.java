package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Statistiques du référentiel.
 *
 * @param total total
 * @param actifs actifs
 * @param enAttente en attente de validation
 * @param suspendus suspendus
 * @param blacklistes blacklistés
 * @param scoreMoyen score moyen des actifs
 * @param litigesOuverts litiges ouverts
 * @param documentsExpires documents expirés
 */
@Schema(description = "Statistiques des fournisseurs")
public record StatistiquesFournisseursDto(
    @Schema(example = "7") long total,
    @Schema(example = "5") long actifs,
    @Schema(example = "1") long enAttente,
    @Schema(example = "1") long suspendus,
    @Schema(example = "0") long blacklistes,
    @Schema(description = "Score moyen des fournisseurs actifs", example = "76.2")
        BigDecimal scoreMoyen,
    @Schema(example = "3") long litigesOuverts,
    @Schema(example = "2") long documentsExpires) {}
