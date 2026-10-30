package sn.cse.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import sn.cse.events.EventEnvelope;

/** Lecture des messages Kafka (JSON) en enveloppes et payloads typés. */
public class EventEnvelopeReader {

  private final ObjectMapper mapper;

  /**
   * @param mapper sérialiseur configuré (JSR-310)
   */
  public EventEnvelopeReader(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * @param message valeur du message Kafka
   * @return l'enveloppe
   * @throws IllegalArgumentException si le message est illisible (non rejoué, envoyé en DLT)
   */
  public EventEnvelope lire(String message) {
    try {
      return mapper.readValue(message, EventEnvelope.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Message Kafka illisible", e);
    }
  }

  /**
   * @param envelope enveloppe lue
   * @param type classe du payload
   * @param <T> type du payload
   * @return le payload typé
   */
  public <T> T payload(EventEnvelope envelope, Class<T> type) {
    return envelope.payloadAs(type, mapper);
  }
}
