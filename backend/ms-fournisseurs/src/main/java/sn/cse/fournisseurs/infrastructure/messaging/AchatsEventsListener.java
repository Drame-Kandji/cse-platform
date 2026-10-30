package sn.cse.fournisseurs.infrastructure.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.ContratPayload;
import sn.cse.fournisseurs.application.command.LivraisonCommand;
import sn.cse.fournisseurs.application.port.in.SuiviPerformanceUseCase;

/**
 * Consommateur idempotent de {@code cse.achats.events} : {@code LIVRAISON_EFFECTUEE} alimente le
 * taux de livraison à l'heure puis déclenche le recalcul du score (R07).
 */
@Component
public class AchatsEventsListener {

  static final String CONSOMMATEUR = "ms-fournisseurs.livraison-effectuee";

  private final EventEnvelopeReader lecteur;
  private final IdempotentEventHandler idempotence;
  private final SuiviPerformanceUseCase performance;

  /**
   * @param lecteur lecteur d'enveloppes
   * @param idempotence garde d'idempotence
   * @param performance cas d'utilisation
   */
  public AchatsEventsListener(
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
  @KafkaListener(topics = Topics.ACHATS, groupId = "${spring.application.name}")
  public void recevoir(String message) {
    EventEnvelope enveloppe = lecteur.lire(message);
    if (!EventTypes.LIVRAISON_EFFECTUEE.equals(enveloppe.type())) {
      return;
    }
    ContratPayload contrat = lecteur.payload(enveloppe, ContratPayload.class);
    if (contrat.fournisseurId() == null) {
      return;
    }
    idempotence.traiterUneFois(
        enveloppe.eventId(),
        CONSOMMATEUR,
        () ->
            performance.enregistrerLivraison(
                new LivraisonCommand(
                    contrat.fournisseurId(),
                    contrat.numero(),
                    contrat.dateLivraisonPrevue(),
                    contrat.dateLivraisonReelle())));
  }
}
