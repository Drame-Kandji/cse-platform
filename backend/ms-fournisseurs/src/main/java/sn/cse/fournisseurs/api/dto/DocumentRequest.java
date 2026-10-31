package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import sn.cse.fournisseurs.domain.model.TypeDocument;

/**
 * Ajout d'un document administratif.
 *
 * @param type type
 * @param libelle libellé
 * @param dateExpiration date d'expiration
 */
@Schema(description = "Document administratif à enregistrer")
public record DocumentRequest(
    @Schema(description = "Type de document", example = "QUITUS_FISCAL") @NotNull TypeDocument type,
    @Schema(description = "Libellé", example = "Quitus fiscal 2026 · DGID")
        @NotBlank
        @Size(max = 200)
        String libelle,
    @Schema(description = "Date d'expiration", example = "2026-12-31") LocalDate dateExpiration) {}
