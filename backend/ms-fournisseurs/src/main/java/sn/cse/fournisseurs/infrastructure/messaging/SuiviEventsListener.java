package sn.cse.fournisseurs.infrastructure.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.AffectationPayload;
import sn.cse.fournisseurs.application.port.in.SuiviPerformanceUseCase;

/**
 * Consommateur idempotent de {@code cse.suivi.events} : {@code RECEPTION_CONFIRMEE} portant un
 * {@code fournisseurId} déclenche le recalcul du score (R07).
 */
@Component
public class SuiviEventsListener {

  static final String CONSOMMATEUR = "ms-fournisseurs.reception-confirmee";

  private final EventEnvelopeReader lecteur;
  private final IdempotentEventHandler idempotence;
  private final SuiviPerformanceUseCase performance;

  /**
   * @param lecteur lecteur d'enveloppes
   * @param idempotence garde d'idempotence
   * @param performance cas d'utilisation
   */
  public SuiviEventsListener(
      EventEnvelopeReader lecteur,
      IdempotentEventHandler idempotence,
      SuiviPerformanceUseCase performance) {
    this.lecteur = lecteur;
    this.idempotence = idempotence;
    this.performance = performance;
  }

  /**
   * @param message enveloppe JSON
   */
  @KafkaListener(topics = Topics.SUIVI, groupId = "${spring.application.name}")
  public void recevoir(String message) {
    EventEnvelope enveloppe = lecteur.lire(message);
    if (!EventTypes.RECEPTION_CONFIRMEE.equals(enveloppe.type())) {
      return;
    }
    AffectationPayload affectation = lecteur.payload(enveloppe, AffectationPayload.class);
    if (affectation.fournisseurId() == null) {
      return;
    }
    idempotence.traiterUneFois(
        enveloppe.eventId(),
        CONSOMMATEUR,
        () -> performance.receptionConfirmee(affectation.fournisseurId()));
  }
}
