package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Comparatif des offres.
 *
 * @param appelOffreId appel d'offres
 * @param criteres grille
 * @param offres offres triées par rang
 * @param recommandationOffreId offre recommandée (rang 1)
 */
@Schema(name = "Comparatif", description = "Comparatif multicritère des offres")
public record ComparatifDto(
    @Schema(example = "d0000000-0000-0000-0000-000000000001") UUID appelOffreId,
    List<CriterePondereDto> criteres,
    List<OffreDto> offres,
    @Schema(example = "6b1f6c1e-2a41-4d63-9a59-1f3e9b7f0c11") UUID recommandationOffreId) {}
