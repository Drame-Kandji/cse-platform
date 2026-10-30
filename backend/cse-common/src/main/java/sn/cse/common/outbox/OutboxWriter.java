package sn.cse.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.CorrelationIdFilter;
import sn.cse.events.Actor;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.AuditPayload;

/**
 * Écrit les événements de domaine dans la table {@code outbox_event}, dans la transaction métier en
 * cours : l'événement n'est publié sur Kafka que si la modification en base est validée (pattern
 * Transactional Outbox). Chaque événement produit aussi automatiquement son entrée d'audit, ce qui
 * garantit la traçabilité de toute modification métier sans code dupliqué (ADR-0007).
 */
public class OutboxWriter {

  private static final String INSERT =
      "INSERT INTO outbox_event (id, topic, message_key, event_type, payload, created_at)"
          + " VALUES (?, ?, ?, ?, ?, ?)";

  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final CurrentUserProvider users;
  private final Clock clock;
  private final String source;

  /**
   * @param jdbc accès JDBC (même connexion que la transaction JPA)
   * @param mapper sérialiseur JSON
   * @param users utilisateur courant (auteur)
   * @param clock horloge
   * @param source nom du microservice émetteur
   */
  public OutboxWriter(
      JdbcTemplate jdbc,
      ObjectMapper mapper,
      CurrentUserProvider users,
      Clock clock,
      String source) {
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.users = users;
    this.clock = clock;
    this.source = source;
  }

  /**
   * Enregistre un événement de domaine et son entrée d'audit.
   *
   * @param topic topic cible (voir {@link Topics})
   * @param type type d'événement (voir {@link EventTypes})
   * @param aggregateType type d'agrégat
   * @param aggregateId identifiant de l'agrégat (clé Kafka)
   * @param payload payload métier
   * @param resume résumé lisible pour le journal d'audit (peut être {@code null})
   * @throws org.springframework.transaction.IllegalTransactionStateException hors transaction
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void publier(
      String topic,
      String type,
      String aggregateType,
      Object aggregateId,
      Object payload,
      String resume) {
    String id = String.valueOf(aggregateId);
    inserer(topic, enveloppe(type, aggregateType, id, payload));
    AuditPayload audit = new AuditPayload(source, type, aggregateType, id, resume);
    inserer(Topics.AUDIT, enveloppe(EventTypes.AUDIT, aggregateType, id, audit));
  }

  /**
   * Variante sans résumé d'audit.
   *
   * @see #publier(String, String, String, Object, Object, String)
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void publier(
      String topic, String type, String aggregateType, Object aggregateId, Object payload) {
    publier(topic, type, aggregateType, aggregateId, payload, null);
  }

  private EventEnvelope enveloppe(String type, String aggregateType, String id, Object payload) {
    return new EventEnvelope(
        UUID.randomUUID(),
        type,
        EventEnvelope.CURRENT_VERSION,
        Instant.now(clock),
        source,
        aggregateType,
        id,
        MDC.get(CorrelationIdFilter.MDC_KEY),
        users.current().map(OutboxWriter::acteur).orElse(Actor.SYSTEME),
        mapper.valueToTree(payload));
  }

  private void inserer(String topic, EventEnvelope env) {
    try {
      jdbc.update(
          INSERT,
          env.eventId(),
          topic,
          env.aggregateId(),
          env.type(),
          mapper.writeValueAsString(env),
          Timestamp.from(env.occurredAt()));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Sérialisation impossible de l'événement " + env.type(), e);
    }
  }

  private static Actor acteur(CurrentUser user) {
    return new Actor(user.id().toString(), user.nom(), new ArrayList<>(user.roles()));
  }
}
