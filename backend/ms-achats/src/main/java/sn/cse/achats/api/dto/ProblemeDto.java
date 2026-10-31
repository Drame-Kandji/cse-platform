package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Documentation du format d'erreur RFC 7807 renvoyé par toutes les routes.
 *
 * @param type URI du type d'erreur
 * @param title titre
 * @param status statut HTTP
 * @param detail message en français
 * @param code code stable
 * @param traceId identifiant de corrélation
 * @param timestamp horodatage
 * @param errors erreurs de validation par champ
 */
@Schema(
    name = "ProblemDetail",
    description = "Erreur au format RFC 7807 (application/problem+json)")
public record ProblemeDto(
    @Schema(example = "https://cse.sn/erreurs/date-limite-depassee") String type,
    @Schema(example = "Règle de gestion non respectée") String title,
    @Schema(example = "422") int status,
    @Schema(example = "La date limite de dépôt des offres est dépassée (R02).") String detail,
    @Schema(example = "DATE_LIMITE_DEPASSEE") String code,
    @Schema(example = "3f6c1a2b-9d8e-4f70-b1a2-c3d4e5f60718") String traceId,
    @Schema(example = "2026-10-30T09:15:00Z") Instant timestamp,
    List<ErreurChamp> errors) {

  /**
   * Erreur de validation d'un champ.
   *
   * @param field champ
   * @param message message
   */
  @Schema(name = "ErreurChamp", description = "Erreur de validation d'un champ")
  public record ErreurChamp(
      @Schema(example = "montantPropose") String field,
      @Schema(example = "doit être supérieur à 0") String message) {}
}
