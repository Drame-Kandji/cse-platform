package sn.cse.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Enveloppe commune à tous les événements publiés sur Kafka. La clé du message Kafka est {@link
 * #aggregateId()} afin de garantir l'ordre des événements d'un même agrégat.
 *
 * @param eventId identifiant unique (sert à l'idempotence des consommateurs)
 * @param type type d'événement (voir {@link EventTypes})
 * @param version version du schéma du payload
 * @param occurredAt instant de survenue
 * @param source microservice émetteur
 * @param aggregateType type d'agrégat (ex. {@code BesoinMateriel})
 * @param aggregateId identifiant de l'agrégat
 * @param correlationId identifiant de corrélation de la requête d'origine
 * @param actor auteur de l'action
 * @param payload contenu métier sérialisé
 */
public record EventEnvelope(
    UUID eventId,
    String type,
    int version,
    Instant occurredAt,
    String source,
    String aggregateType,
    String aggregateId,
    String correlationId,
    Actor actor,
    JsonNode payload) {

  /** Version courante des schémas de payload. */
  public static final int CURRENT_VERSION = 1;

  public EventEnvelope {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(occurredAt, "occurredAt");
    Objects.requireNonNull(aggregateId, "aggregateId");
    actor = actor == null ? Actor.SYSTEME : actor;
  }

  /**
   * Désérialise le payload dans le type attendu.
   *
   * @param type classe du payload
   * @param mapper mapper Jackson configuré (module JSR-310)
   * @param <T> type du payload
   * @return le payload typé
   * @throws IllegalArgumentException si le payload ne correspond pas au type
   */
  public <T> T payloadAs(Class<T> type, ObjectMapper mapper) {
    try {
      return mapper.treeToValue(payload, type);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Payload incompatible pour l'événement " + this.type + " (" + eventId + ")", e);
    }
  }
}
