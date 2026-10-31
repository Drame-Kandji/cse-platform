package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Fournisseur invité.
 *
 * @param id identifiant
 * @param raisonSociale raison sociale
 */
@Schema(name = "FournisseurInvite", description = "Fournisseur invité à l'appel d'offres")
public record FournisseurInviteDto(
    @Schema(example = "f0000000-0000-0000-0000-000000000001") UUID id,
    @Schema(example = "Sahel Engins SA") String raisonSociale) {}
