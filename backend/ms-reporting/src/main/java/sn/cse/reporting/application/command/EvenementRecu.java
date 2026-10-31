package sn.cse.reporting.application.command;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Événement de domaine reçu d'un autre contexte, indépendant de la technologie de transport.
 *
 * @param eventId identifiant unique (idempotence)
 * @param type type d'événement ({@code sn.cse.events.EventTypes})
 * @param occurredAt instant de survenue
 * @param lecteur accès typé au payload
 */
public record EvenementRecu(UUID eventId, String type, Instant occurredAt, LecteurPayload lecteur) {

  public EvenementRecu {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(occurredAt, "occurredAt");
    Objects.requireNonNull(lecteur, "lecteur");
  }

  /**
   * @param classe classe du payload
   * @param <T> type du payload
   * @return le payload typé
   */
  public <T> T payload(Class<T> classe) {
    return lecteur.lire(classe);
  }
}
