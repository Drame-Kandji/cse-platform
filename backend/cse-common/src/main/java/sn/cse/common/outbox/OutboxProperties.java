package sn.cse.common.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres de l'outbox transactionnelle ({@code cse.outbox.*}).
 *
 * @param relayEnabled active le relais vers Kafka (désactivé dans les tests sans broker)
 * @param batchSize nombre maximal d'événements relayés par passage
 * @param sendTimeout délai d'acquittement Kafka
 * @param retention durée de conservation des événements déjà publiés
 */
@ConfigurationProperties(prefix = "cse.outbox")
public record OutboxProperties(
    Boolean relayEnabled, Integer batchSize, Duration sendTimeout, Duration retention) {

  public OutboxProperties {
    relayEnabled = relayEnabled == null || relayEnabled;
    batchSize = batchSize == null ? 100 : batchSize;
    sendTimeout = sendTimeout == null ? Duration.ofSeconds(5) : sendTimeout;
    retention = retention == null ? Duration.ofDays(7) : retention;
  }
}
