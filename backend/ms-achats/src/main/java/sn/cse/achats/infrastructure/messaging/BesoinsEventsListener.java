package sn.cse.achats.infrastructure.messaging;

import java.time.Instant;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sn.cse.achats.application.command.BesoinEnAchatCommand;
import sn.cse.achats.application.port.in.MettreAJourProjectionsUseCase;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.Priorite;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.BesoinPayload;

/**
 * Consommateur idempotent de {@code cse.besoins.events} : seuls les besoins orientés vers l'achat
 * ({@code BESOIN_EN_ACHAT}) alimentent la projection des besoins à acheter.
 */
@Component
public class BesoinsEventsListener {

  static final String CONSOMMATEUR = "ms-achats.besoins-a-acheter";

  private final EventEnvelopeReader lecteur;
  private final IdempotentEventHandler idempotence;
  private final MettreAJourProjectionsUseCase projections;

  /**
   * @param lecteur lecteur d'enveloppes
   * @param idempotence garde d'idempotence
   * @param projections cas d'utilisation des projections
   */
  public BesoinsEventsListener(
      EventEnvelopeReader lecteur,
      IdempotentEventHandler idempotence,
      MettreAJourProjectionsUseCase projections) {
    this.lecteur = lecteur;
    this.idempotence = idempotence;
    this.projections = projections;
  }

  /**
   * @param message enveloppe JSON
   */
  @KafkaListener(topics = Topics.BESOINS, groupId = "${spring.application.name}")
  public void recevoir(String message) {
    EventEnvelope env = lecteur.lire(message);
    if (!EventTypes.BESOIN_EN_ACHAT.equals(env.type())) {
      return;
    }
    BesoinPayload p = lecteur.payload(env, BesoinPayload.class);
    BesoinEnAchatCommand commande =
        new BesoinEnAchatCommand(
            p.besoinId(),
            p.reference(),
            p.designation(),
            p.categorie() == null ? null : CategorieMateriel.valueOf(p.categorie()),
            p.quantite(),
            p.specifications(),
            p.priorite() == null ? null : Priorite.valueOf(p.priorite()),
            p.chantierId(),
            p.chantierNom(),
            env.occurredAt() == null ? Instant.EPOCH : env.occurredAt());
    idempotence.traiterUneFois(
        env.eventId(), CONSOMMATEUR, () -> projections.besoinEnAchat(commande));
  }
}
