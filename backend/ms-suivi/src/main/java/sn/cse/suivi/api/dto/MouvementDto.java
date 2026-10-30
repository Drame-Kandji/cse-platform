package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import sn.cse.suivi.domain.model.TypeMouvement;

/**
 * Mouvement de l'historique d'un matériel.
 *
 * @param id identifiant
 * @param materielId matériel
 * @param type nature
 * @param chantierOrigineNom chantier quitté
 * @param chantierDestinationNom chantier rejoint
 * @param date instant
 * @param auteurNom auteur
 * @param commentaire commentaire
 */
@Schema(name = "Mouvement", description = "Mouvement tracé d'un matériel")
public record MouvementDto(
    @Schema(example = "9c8b7a6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d") UUID id,
    @Schema(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") UUID materielId,
    @Schema(example = "AFFECTATION") TypeMouvement type,
    @Schema(example = "Contournement de Thiès RN2", nullable = true) String chantierOrigineNom,
    @Schema(example = "Extension autoroute Dakar–AIBD", nullable = true)
        String chantierDestinationNom,
    @Schema(example = "2026-10-30T09:15:00Z") Instant date,
    @Schema(example = "Ibrahima Fall", nullable = true) String auteurNom,
    @Schema(example = "Affectation du 2026-11-02 au 2027-02-28", nullable = true)
        String commentaire) {}
