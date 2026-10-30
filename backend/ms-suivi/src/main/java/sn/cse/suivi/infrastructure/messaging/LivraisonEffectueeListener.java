package sn.cse.suivi.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.ContratPayload;
import sn.cse.suivi.application.command.LivraisonRecue;
import sn.cse.suivi.application.port.in.EnregistrerLivraisonUseCase;
import sn.cse.suivi.domain.model.CategorieMateriel;

/**
 * Consomme {@code LIVRAISON_EFFECTUEE} (topic {@code cse.achats.events}) une seule fois par
 * événement ; les autres types du topic sont ignorés. Une catégorie inconnue rend le message
 * illisible : il part directement en DLT, sans rejeu.
 */
@Component
public class LivraisonEffectueeListener {

  /** Nom logique du consommateur (clé d'idempotence). */
  public static final String CONSOMMATEUR = "ms-suivi.livraison-effectuee";

  private static final Logger LOG = LoggerFactory.getLogger(LivraisonEffectueeListener.class);

  private final EventEnvelopeReader lecteur;
  private final IdempotentEventHandler idempotence;
  private final EnregistrerLivraisonUseCase livraisons;

  /**
   * @param lecteur lecteur d'enveloppes
   * @param idempotence garde d'idempotence
   * @param livraisons cas d'utilisation
   */
  public LivraisonEffectueeListener(
      EventEnvelopeReader lecteur,
      IdempotentEventHandler idempotence,
      EnregistrerLivraisonUseCase livraisons) {
    this.lecteur = lecteur;
    this.idempotence = idempotence;
    this.livraisons = livraisons;
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
    LivraisonRecue livraison = versLivraison(contrat);
    boolean traite =
        idempotence.traiterUneFois(
            enveloppe.eventId(), CONSOMMATEUR, () -> livraisons.enregistrer(livraison));
    if (!traite) {
      LOG.info("Livraison {} déjà intégrée (événement {})", contrat.numero(), enveloppe.eventId());
    }
  }

  static LivraisonRecue versLivraison(ContratPayload c) {
    return new LivraisonRecue(
        c.contratId(),
        c.numero(),
        c.besoinId(),
        c.chantierId(),
        c.fournisseurId(),
        c.designation(),
        CategorieMateriel.valueOf(c.categorie()),
        c.quantite(),
        c.montant(),
        c.tarifJournalierEstime(),
        c.dateLivraisonReelle());
  }
}
