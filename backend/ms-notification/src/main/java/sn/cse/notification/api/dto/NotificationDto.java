package sn.cse.notification.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.domain.model.Notification;
import sn.cse.notification.domain.model.NotificationVue;

/**
 * Notification exposée au frontend (contrat §7).
 *
 * @param id identifiant
 * @param type type d'événement d'origine
 * @param titre titre
 * @param message message
 * @param niveau criticité
 * @param lien route frontend
 * @param entite type d'entité
 * @param entiteId identifiant de l'entité
 * @param lue état de lecture pour l'utilisateur courant
 * @param createdAt date de création
 */
@Schema(
    name = "Notification",
    description = "Notification destinée à l'utilisateur ou à un de ses rôles")
public record NotificationDto(
    @Schema(example = "6f1c2a8e-4b1d-4c7e-9a51-0d3f2b7c9e10") UUID id,
    @Schema(description = "Type de l'événement d'origine", example = "BESOIN_VALIDE") String type,
    @Schema(example = "Besoin BES-2026-0012 validé") String titre,
    @Schema(
            example =
                "Votre besoin BES-2026-0012 (2 × Pelle hydraulique) a été validé par Ibrahima Fall.")
        String message,
    @Schema(example = "SUCCES") NiveauNotification niveau,
    @Schema(
            description = "Route du frontend",
            example = "/besoins/3c9d8f6e-1a2b-4c3d-8e9f-0a1b2c3d4e5f",
            nullable = true)
        String lien,
    @Schema(example = "BESOIN", nullable = true) String entite,
    @Schema(example = "3c9d8f6e-1a2b-4c3d-8e9f-0a1b2c3d4e5f", nullable = true) String entiteId,
    @Schema(example = "false") boolean lue,
    @Schema(example = "2026-10-30T09:15:00Z") Instant createdAt) {

  /**
   * @param vue notification vue par l'utilisateur
   * @return le DTO
   */
  public static NotificationDto depuis(NotificationVue vue) {
    return depuis(vue.notification(), vue.lue());
  }

  /**
   * @param n notification
   * @param lue état de lecture
   * @return le DTO
   */
  public static NotificationDto depuis(Notification n, boolean lue) {
    return new NotificationDto(
        n.id(),
        n.type(),
        n.titre(),
        n.message(),
        n.niveau(),
        n.lien(),
        n.entite(),
        n.entiteId(),
        lue,
        n.createdAt());
  }
}
