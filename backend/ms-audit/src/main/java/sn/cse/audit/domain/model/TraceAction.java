package sn.cse.audit.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Description d'une action auditée telle que reçue d'un service émetteur (enveloppe + payload
 * {@code AuditPayload}), avant son enregistrement au journal.
 *
 * @param eventId identifiant de l'événement d'audit
 * @param occurredAt instant de l'action
 * @param service service émetteur
 * @param action action réalisée
 * @param entite type d'entité
 * @param entiteId identifiant de l'entité
 * @param utilisateurId auteur, {@code null} si action système
 * @param utilisateurNom nom de l'auteur
 * @param roles rôles de l'auteur
 * @param resume résumé lisible
 * @param correlationId identifiant de corrélation
 */
public record TraceAction(
    UUID eventId,
    Instant occurredAt,
    String service,
    String action,
    String entite,
    String entiteId,
    String utilisateurId,
    String utilisateurNom,
    List<String> roles,
    String resume,
    String correlationId) {

  public TraceAction {
    roles = roles == null ? List.of() : roles.stream().filter(Objects::nonNull).toList();
  }
}
