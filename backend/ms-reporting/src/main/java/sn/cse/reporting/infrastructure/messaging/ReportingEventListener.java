package sn.cse.reporting.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.Topics;
import sn.cse.reporting.application.command.EvenementRecu;
import sn.cse.reporting.application.command.LecteurPayload;
import sn.cse.reporting.application.port.in.ProjeterAchatsUseCase;
import sn.cse.reporting.application.port.in.ProjeterBesoinsUseCase;
import sn.cse.reporting.application.port.in.ProjeterEvenementUseCase;
import sn.cse.reporting.application.port.in.ProjeterFournisseursUseCase;
import sn.cse.reporting.application.port.in.ProjeterSuiviUseCase;
import sn.cse.reporting.infrastructure.config.ReportingProperties;

/**
 * Consommateurs Kafka des quatre contextes métier. Chaque message est traité une seule fois par
 * topic ({@code processed_event}, consommateur {@code <prefixe>.<topic>}) ; rejeu puis DLT gérés
 * par {@code cse-common}. Les types d'événements non pris en charge sont ignorés.
 */
@Component
public class ReportingEventListener {

  private static final Logger LOG = LoggerFactory.getLogger(ReportingEventListener.class);

  private final EventEnvelopeReader reader;
  private final IdempotentEventHandler idempotence;
  private final ProjeterSuiviUseCase suivi;
  private final ProjeterBesoinsUseCase besoins;
  private final ProjeterFournisseursUseCase fournisseurs;
  private final ProjeterAchatsUseCase achats;
  private final String prefixe;

  /**
   * @param reader lecteur d'enveloppes
   * @param idempotence garde d'idempotence
   * @param suivi projection ms-suivi
   * @param besoins projection ms-besoins
   * @param fournisseurs projection ms-fournisseurs
   * @param achats projection ms-achats
   * @param properties configuration (préfixe du consommateur)
   */
  public ReportingEventListener(
      EventEnvelopeReader reader,
      IdempotentEventHandler idempotence,
      ProjeterSuiviUseCase suivi,
      ProjeterBesoinsUseCase besoins,
      ProjeterFournisseursUseCase fournisseurs,
      ProjeterAchatsUseCase achats,
      ReportingProperties properties) {
    this.reader = reader;
    this.idempotence = idempotence;
    this.suivi = suivi;
    this.besoins = besoins;
    this.fournisseurs = fournisseurs;
    this.achats = achats;
    this.prefixe = properties.consommateur();
  }

  /**
   * @param message enveloppe JSON de {@code cse.suivi.events}
   */
  @KafkaListener(topics = Topics.SUIVI, groupId = "${spring.application.name}")
  public void surSuivi(String message) {
    traiter(Topics.SUIVI, message, suivi);
  }

  /**
   * @param message enveloppe JSON de {@code cse.besoins.events}
   */
  @KafkaListener(topics = Topics.BESOINS, groupId = "${spring.application.name}")
  public void surBesoins(String message) {
    traiter(Topics.BESOINS, message, besoins);
  }

  /**
   * @param message enveloppe JSON de {@code cse.fournisseurs.events}
   */
  @KafkaListener(topics = Topics.FOURNISSEURS, groupId = "${spring.application.name}")
  public void surFournisseurs(String message) {
    traiter(Topics.FOURNISSEURS, message, fournisseurs);
  }

  /**
   * @param message enveloppe JSON de {@code cse.achats.events}
   */
  @KafkaListener(topics = Topics.ACHATS, groupId = "${spring.application.name}")
  public void surAchats(String message) {
    traiter(Topics.ACHATS, message, achats);
  }

  private void traiter(String topic, String message, ProjeterEvenementUseCase projection) {
    EventEnvelope env = reader.lire(message);
    LecteurPayload lecteur =
        new LecteurPayload() {
          @Override
          public <T> T lire(Class<T> type) {
            return reader.payload(env, type);
          }
        };
    EvenementRecu evenement =
        new EvenementRecu(env.eventId(), env.type(), env.occurredAt(), lecteur);
    idempotence.traiterUneFois(
        env.eventId(),
        prefixe + "." + topic,
        () -> {
          if (!projection.projeter(evenement)) {
            LOG.debug("Type d'événement {} ignoré sur {}", env.type(), topic);
          }
        });
  }
}
