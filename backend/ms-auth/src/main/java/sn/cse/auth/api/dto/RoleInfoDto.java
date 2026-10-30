package sn.cse.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sn.cse.auth.domain.model.Role;

/**
 * Description d'un rôle (contrat §9 {@code RoleInfo}).
 *
 * @param code code du rôle
 * @param libelle libellé français
 * @param description périmètre fonctionnel
 * @param nombreUtilisateurs nombre de comptes portant ce rôle
 * @param otpObligatoire second facteur exigé
 */
@Schema(name = "RoleInfo", description = "Rôle de la plateforme et ses effectifs")
public record RoleInfoDto(
    @Schema(example = "DAF") Role code,
    @Schema(example = "Direction Administrative et Financière") String libelle,
    @Schema(example = "Engagements budgétaires, mandatement et paiement des factures")
        String description,
    @Schema(example = "1") long nombreUtilisateurs,
    @Schema(example = "true") boolean otpObligatoire) {}
