package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.achats.domain.model.StatutJalon;

/**
 * Jalon de contrat.
 *
 * @param id identifiant
 * @param libelle libellé
 * @param datePrevue échéance
 * @param dateReelle réalisation
 * @param statut statut au jour de la consultation
 */
@Schema(name = "Jalon", description = "Jalon d'exécution du contrat")
public record JalonDto(
    @Schema(example = "0e7c7d3c-90b4-4e3a-8f4b-4a7e3b1f9a10") UUID id,
    @Schema(example = "Expédition depuis le port de Dakar") String libelle,
    @Schema(example = "2026-11-10") LocalDate datePrevue,
    @Schema(example = "2026-11-08") LocalDate dateReelle,
    @Schema(example = "ATTEINT") StatutJalon statut) {}
