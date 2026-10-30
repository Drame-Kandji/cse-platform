package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Documentation du format d'erreur RFC 7807 renvoyé par l'API ({@code application/problem+json}).
 *
 * @param type URI du type d'erreur
 * @param title titre court
 * @param status statut HTTP
 * @param detail message en français
 * @param code code d'erreur stable
 * @param traceId identifiant de corrélation
 * @param timestamp horodatage
 * @param errors erreurs de validation par champ
 */
@Schema(name = "Probleme", description = "Erreur au format RFC 7807 (ProblemDetail)")
public record ProblemeDto(
    @Schema(example = "https://cse.sn/erreurs/conflit-affectation") String type,
    @Schema(example = "Conflit") String title,
    @Schema(example = "409") int status,
    @Schema(
            example =
                "Le matériel PEL-CAT320-01 est déjà affecté sur une période qui chevauche celle"
                    + " demandée.")
        String detail,
    @Schema(example = "CONFLIT_AFFECTATION") String code,
    @Schema(example = "6f1c2a7e-3b0d-4d8e-9a51-2f7c1b9e4d10") String traceId,
    @Schema(example = "2026-10-30T09:15:00Z") Instant timestamp,
    @Schema(description = "Erreurs de validation (400 uniquement)") List<ErreurChamp> errors) {

  /**
   * @param field champ en erreur
   * @param message message
   */
  public record ErreurChamp(
      @Schema(example = "dateFinPrevue") String field,
      @Schema(example = "ne doit pas être nul") String message) {}
}
