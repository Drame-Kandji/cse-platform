package sn.cse.audit.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import sn.cse.audit.domain.model.AuditLog;

/**
 * Entrée du journal d'audit exposée par l'API (contrat §8).
 *
 * @param id identifiant de l'entrée
 * @param eventId identifiant de l'événement d'audit
 * @param occurredAt instant de l'action
 * @param service service émetteur
 * @param action action réalisée
 * @param entite type d'entité
 * @param entiteId identifiant de l'entité
 * @param utilisateurId auteur
 * @param utilisateurNom nom de l'auteur
 * @param roles rôles de l'auteur
 * @param resume résumé lisible
 * @param correlationId identifiant de corrélation
 */
@Schema(name = "AuditLog", description = "Entrée immuable du journal d'audit")
public record AuditLogDto(
    @Schema(
            description = "Identifiant de l'entrée",
            example = "3f1c2a9e-7b1d-4c55-9a0e-1d2b3c4d5e6f")
        UUID id,
    @Schema(
            description = "Identifiant de l'événement d'audit d'origine",
            example = "9b2e4f10-2c3d-4e5f-8a9b-0c1d2e3f4a5b")
        UUID eventId,
    @Schema(description = "Instant de l'action (UTC)", example = "2026-10-30T09:15:00Z")
        Instant occurredAt,
    @Schema(description = "Microservice émetteur", example = "ms-besoins") String service,
    @Schema(description = "Action réalisée (type d'événement)", example = "BESOIN_VALIDE")
        String action,
    @Schema(description = "Type d'entité concernée", example = "BesoinMateriel") String entite,
    @Schema(
            description = "Identifiant de l'entité concernée",
            example = "b0000000-0000-0000-0000-000000000001")
        String entiteId,
    @Schema(
            description = "Identifiant Keycloak de l'auteur (absent pour une action système)",
            example = "00000000-0000-0000-0000-000000000201",
            nullable = true)
        String utilisateurId,
    @Schema(description = "Nom de l'auteur", example = "Ibrahima Fall", nullable = true)
        String utilisateurNom,
    @ArraySchema(
            schema =
                @Schema(
                    description = "Rôle de l'auteur au moment de l'action",
                    example = "RESPONSABLE_MATERIEL"))
        List<String> roles,
    @Schema(
            description = "Résumé lisible de l'action",
            example = "Besoin BES-2026-0001 validé",
            nullable = true)
        String resume,
    @Schema(
            description = "Identifiant de corrélation de la requête d'origine",
            example = "c7d8e9f0-1a2b-3c4d-5e6f-7a8b9c0d1e2f",
            nullable = true)
        String correlationId) {

  /**
   * @param log entrée du domaine
   * @return le DTO
   */
  public static AuditLogDto depuis(AuditLog log) {
    return new AuditLogDto(
        log.id(),
        log.eventId(),
        log.occurredAt(),
        log.service(),
        log.action(),
        log.entite(),
        log.entiteId(),
        log.utilisateurId(),
        log.utilisateurNom(),
        log.roles(),
        log.resume(),
        log.correlationId());
  }
}
