package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import sn.cse.achats.domain.model.StatutApprobation;

/**
 * Décision d'un approbateur.
 *
 * @param statut statut
 * @param parNom décideur
 * @param date instant de la décision
 * @param commentaire commentaire
 */
@Schema(name = "Approbation", description = "Décision d'un approbateur (R11)")
public record ApprobationDto(
    @Schema(example = "APPROUVEE") StatutApprobation statut,
    @Schema(example = "Ibrahima Fall") String parNom,
    @Schema(example = "2026-10-12T10:30:00Z") Instant date,
    @Schema(example = "Conforme au besoin exprimé par le chantier.") String commentaire) {}
