package sn.cse.auth.infrastructure.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sn.cse.auth.application.command.ActiverCompteFournisseurCommand;
import sn.cse.auth.application.port.in.ActiverCompteFournisseurUseCase;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.FournisseurPayload;

/**
 * Consommateur idempotent de {@code cse.fournisseurs.events} : {@code FOURNISSEUR_VALIDE} crée le
 * compte du portail du fournisseur (contrat §13). Une erreur Keycloak est propagée : rejeu puis
 * Dead Letter Topic (gestionnaire d'erreurs de cse-common), la marque d'idempotence étant annulée
 * avec la transaction.
 */
@Component
public class FournisseursEventsListener {

  static final String CONSOMMATEUR = "ms-auth.fournisseur-valide";

  private final EventEnvelopeReader lecteur;
  private final IdempotentEventHandler idempotence;
  private final ActiverCompteFournisseurUseCase activation;

  /**
   * @param lecteur lecteur d'enveloppes
   * @param idempotence garde d'idempotence
   * @param activation cas d'utilisation
   */
  public FournisseursEventsListener(
      EventEnvelopeReader lecteur,
      IdempotentEventHandler idempotence,
      ActiverCompteFournisseurUseCase activation) {
    this.lecteur = lecteur;
    this.idempotence = idempotence;
    this.activation = activation;
  }

  /**
   * @param message enveloppe JSON
   */
  @KafkaListener(topics = Topics.FOURNISSEURS, groupId = "${spring.application.name}")
  public void recevoir(String message) {
    EventEnvelope enveloppe = lecteur.lire(message);
    if (!EventTypes.FOURNISSEUR_VALIDE.equals(enveloppe.type())) {
      return;
    }
    FournisseurPayload fournisseur = lecteur.payload(enveloppe, FournisseurPayload.class);
    // Commande construite hors de la garde : un payload incomplet (IllegalArgumentException) part
    // directement en DLT, sans rejeu.
    ActiverCompteFournisseurCommand commande =
        new ActiverCompteFournisseurCommand(
            fournisseur.fournisseurId(),
            fournisseur.email(),
            fournisseur.raisonSociale(),
            fournisseur.contactNom());
    idempotence.traiterUneFois(
        enveloppe.eventId(), CONSOMMATEUR, () -> activation.activer(commande));
  }
}
