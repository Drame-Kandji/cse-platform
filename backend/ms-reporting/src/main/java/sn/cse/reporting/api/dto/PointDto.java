package sn.cse.reporting.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import sn.cse.reporting.domain.model.Point;

/**
 * Point d'une série temporelle (contrat §6).
 *
 * @param periode libellé de la tranche
 * @param valeur valeur
 */
@Schema(name = "Point", description = "Point d'une série temporelle")
public record PointDto(
    @Schema(
            description =
                "Tranche : « 2026-07 » (MOIS), « 2026-07-14 » (JOUR), « 2026-W29 » (SEMAINE ISO)",
            example = "2026-07")
        String periode,
    @Schema(description = "Valeur de l'indicateur sur la tranche", example = "72.5")
        BigDecimal valeur) {

  /**
   * @param p point du domaine
   * @return le DTO
   */
  public static PointDto depuis(Point p) {
    return new PointDto(p.periode(), p.valeur());
  }
}
