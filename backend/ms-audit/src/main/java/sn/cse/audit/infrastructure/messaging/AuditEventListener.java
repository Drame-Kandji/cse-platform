package sn.cse.audit.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sn.cse.audit.application.port.in.EnregistrerTraceUseCase;
import sn.cse.audit.domain.model.TraceAction;
import sn.cse.audit.infrastructure.config.AuditProperties;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.AuditPayload;

/**
 * Consommateur du topic {@code cse.audit.events}. Chaque message est traité une seule fois
 * (idempotence par {@code processed_event}) ; les messages illisibles ou incomplets partent en Dead
 * Letter Topic sans rejeu, les erreurs techniques sont rejouées (gestionnaire de cse-common).
 */
@Component
public class AuditEventListener {

  private static final Logger LOG = LoggerFactory.getLogger(AuditEventListener.class);

  private final EventEnvelopeReader reader;
  private final IdempotentEventHandler idempotence;
  private final EnregistrerTraceUseCase enregistrer;
  private final String consommateur;

  /**
   * @param reader lecteur d'enveloppes
   * @param idempotence garde d'idempotence
   * @param enregistrer cas d'utilisation d'ajout au journal
   * @param properties configuration (nom du consommateur)
   */
  public AuditEventListener(
      EventEnvelopeReader reader,
      IdempotentEventHandler idempotence,
      EnregistrerTraceUseCase enregistrer,
      AuditProperties properties) {
    this.reader = reader;
    this.idempotence = idempotence;
    this.enregistrer = enregistrer;
    this.consommateur = properties.consommateur();
  }

  /**
   * @param message enveloppe JSON reçue
   */
  @KafkaListener(topics = Topics.AUDIT, groupId = "${spring.application.name}")
  public void recevoir(String message) {
    EventEnvelope env = reader.lire(message);
    if (!EventTypes.AUDIT.equals(env.type())) {
      LOG.debug("Type d'événement {} ignoré sur {}", env.type(), Topics.AUDIT);
      return;
    }
    AuditPayload payload = reader.payload(env, AuditPayload.class);
    TraceAction trace = versTrace(env, payload);
    idempotence.traiterUneFois(env.eventId(), consommateur, () -> enregistrer.enregistrer(trace));
  }

  private static TraceAction versTrace(EventEnvelope env, AuditPayload p) {
    // Le service du payload fait foi ; à défaut, celui de l'enveloppe.
    String service = p.service() == null || p.service().isBlank() ? env.source() : p.service();
    String entite = p.entite() == null || p.entite().isBlank() ? env.aggregateType() : p.entite();
    String entiteId =
        p.entiteId() == null || p.entiteId().isBlank() ? env.aggregateId() : p.entiteId();
    return new TraceAction(
        env.eventId(),
        env.occurredAt(),
        service,
        p.action(),
        entite,
        entiteId,
        env.actor().id(),
        env.actor().nom(),
        env.actor().roles(),
        p.resume(),
        env.correlationId());
  }
}
