package sn.cse.events.payload;

/**
 * Entrée de journal d'audit, publiée automatiquement par l'outbox pour chaque événement de domaine.
 *
 * @param service microservice émetteur
 * @param action action réalisée (type d'événement)
 * @param entite type d'entité concernée
 * @param entiteId identifiant de l'entité
 * @param resume résumé lisible de l'action
 */
public record AuditPayload(
    String service, String action, String entite, String entiteId, String resume) {}
