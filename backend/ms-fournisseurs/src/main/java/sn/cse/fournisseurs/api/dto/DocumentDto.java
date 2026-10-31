package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.fournisseurs.domain.model.TypeDocument;

/**
 * Document administratif.
 *
 * @param id identifiant
 * @param type type
 * @param libelle libellé
 * @param dateExpiration date d'expiration
 * @param expire document expiré à la date du jour
 */
@Schema(description = "Document administratif")
public record DocumentDto(
    @Schema(example = "d0000000-0000-0000-0000-000000000001") UUID id,
    @Schema(example = "QUITUS_FISCAL") TypeDocument type,
    @Schema(example = "Quitus fiscal 2026 · DGID") String libelle,
    @Schema(example = "2026-12-31") LocalDate dateExpiration,
    @Schema(description = "Expiré à la date du jour", example = "false") boolean expire) {}
