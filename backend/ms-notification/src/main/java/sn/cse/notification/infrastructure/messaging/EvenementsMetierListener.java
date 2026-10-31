package sn.cse.notification.infrastructure.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.Topics;
import sn.cse.notification.application.port.in.TraiterEvenementUseCase;
import sn.cse.notification.domain.model.EvenementMetier;

/**
 * Consommateur idempotent des topics métier : chaque enveloppe est convertie en {@link
 * EvenementMetier} puis routée selon la configuration. Rejeu puis DLT gérés par cse-common.
 */
@Component
public class EvenementsMetierListener {

  static final String CONSOMMATEUR = "ms-notification.routage";
  private static final TypeReference<Map<String, Object>> CARTE = new TypeReference<>() {};

  private final EventEnvelopeReader reader;
  private final IdempotentEventHandler idempotence;
  private final TraiterEvenementUseCase useCase;
  private final ObjectMapper mapper;

  /**
   * @param reader lecture des enveloppes
   * @param idempotence garde « une seule fois »
   * @param useCase routage
   * @param mapper conversion du payload
   */
  public EvenementsMetierListener(
      EventEnvelopeReader reader,
      IdempotentEventHandler idempotence,
      TraiterEvenementUseCase useCase,
      ObjectMapper mapper) {
    this.reader = reader;
    this.idempotence = idempotence;
    this.useCase = useCase;
    this.mapper = mapper;
  }

  /**
   * @param message enveloppe JSON
   */
  @KafkaListener(
      topics = {Topics.BESOINS, Topics.SUIVI, Topics.FOURNISSEURS, Topics.ACHATS},
      groupId = "${spring.application.name}")
  public void consommer(String message) {
    EventEnvelope env = reader.lire(message);
    idempotence.traiterUneFois(env.eventId(), CONSOMMATEUR, () -> useCase.traiter(convertir(env)));
  }

  EvenementMetier convertir(EventEnvelope env) {
    Map<String, Object> donnees =
        env.payload() == null || env.payload().isNull()
            ? Map.of()
            : mapper.convertValue(env.payload(), CARTE);
    return new EvenementMetier(
        env.eventId(), env.type(), env.aggregateId(), env.occurredAt(), env.actor().nom(), donnees);
  }
}
