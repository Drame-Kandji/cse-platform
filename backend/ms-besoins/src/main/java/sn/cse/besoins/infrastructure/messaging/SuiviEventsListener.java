package sn.cse.besoins.infrastructure.messaging;

import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sn.cse.besoins.application.command.JournaliserAffectationCommand;
import sn.cse.besoins.application.port.in.JournaliserAffectationUseCase;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.AffectationPayload;

/**
 * Consomme {@code cse.suivi.events} : seules les affectations ({@code MATERIEL_AFFECTE}) liées à un
 * besoin sont historisées. Traitement idempotent (rejeu Kafka) ; les échecs sont rejoués puis
 * envoyés en DLT par le gestionnaire d'erreurs commun.
 */
@Component
public class SuiviEventsListener {

  /** Nom logique du consommateur (table {@code processed_event}). */
  public static final String CONSOMMATEUR = "ms-besoins.historique-affectations";

  private final EventEnvelopeReader lecteur;
  private final IdempotentEventHandler idempotence;
  private final JournaliserAffectationUseCase journalisation;

  /**
   * @param lecteur lecteur d'enveloppes
   * @param idempotence garde d'idempotence
   * @param journalisation cas d'utilisation
   */
  public SuiviEventsListener(
      EventEnvelopeReader lecteur,
      IdempotentEventHandler idempotence,
      JournaliserAffectationUseCase journalisation) {
    this.lecteur = lecteur;
    this.idempotence = idempotence;
    this.journalisation = journalisation;
  }

  /**
   * @param message enveloppe JSON
   */
  @KafkaListener(topics = Topics.SUIVI, groupId = "${spring.application.name}")
  public void recevoir(String message) {
    EventEnvelope enveloppe = lecteur.lire(message);
    if (!EventTypes.MATERIEL_AFFECTE.equals(enveloppe.type())) {
      return;
    }
    AffectationPayload affectation = lecteur.payload(enveloppe, AffectationPayload.class);
    if (affectation.besoinId() == null) {
      return;
    }
    idempotence.traiterUneFois(
        enveloppe.eventId(),
        CONSOMMATEUR,
        () ->
            journalisation.journaliser(
                new JournaliserAffectationCommand(
                    affectation.besoinId(),
                    affectation.materielCode(),
                    affectation.materielDesignation(),
                    affectation.chantierNom(),
                    affectation.dateDebut(),
                    affectation.dateFinPrevue(),
                    auteurId(enveloppe),
                    enveloppe.actor().nom(),
                    enveloppe.occurredAt())));
  }

  private static UUID auteurId(EventEnvelope enveloppe) {
    String id = enveloppe.actor().id();
    if (id == null || id.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      // Acteur technique non identifié par un UUID : l'entrée est attribuée par son seul nom.
      return null;
    }
  }
}
