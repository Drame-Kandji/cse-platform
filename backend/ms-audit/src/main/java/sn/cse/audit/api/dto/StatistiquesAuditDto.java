package sn.cse.audit.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import sn.cse.audit.domain.model.StatistiquesAudit;

/**
 * Statistiques du journal d'audit (contrat §8).
 *
 * @param total nombre total d'entrées
 * @param aujourdhui entrées du jour
 * @param septDerniersJours entrées des sept derniers jours
 * @param parService volume par service
 */
@Schema(name = "StatistiquesAudit", description = "Statistiques globales du journal d'audit")
public record StatistiquesAuditDto(
    @Schema(description = "Nombre total d'entrées", example = "1520") long total,
    @Schema(description = "Entrées dont l'action date du jour (UTC)", example = "42")
        long aujourdhui,
    @Schema(description = "Entrées des sept derniers jours, jour courant inclus", example = "311")
        long septDerniersJours,
    @Schema(description = "Volume par service, décroissant") List<TotalServiceDto> parService) {

  /**
   * @param s statistiques du domaine
   * @return le DTO
   */
  public static StatistiquesAuditDto depuis(StatistiquesAudit s) {
    return new StatistiquesAuditDto(
        s.total(),
        s.aujourdhui(),
        s.septDerniersJours(),
        s.parService().stream().map(t -> new TotalServiceDto(t.service(), t.total())).toList());
  }

  /**
   * Volume d'entrées d'un service.
   *
   * @param service service émetteur
   * @param total nombre d'entrées
   */
  @Schema(name = "TotalService", description = "Volume d'entrées d'un service")
  public record TotalServiceDto(
      @Schema(description = "Service émetteur", example = "ms-achats") String service,
      @Schema(description = "Nombre d'entrées", example = "380") long total) {}
}
