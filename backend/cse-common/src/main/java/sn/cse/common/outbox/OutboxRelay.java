package sn.cse.common.outbox;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Relaie périodiquement les événements en attente de l'outbox vers Kafka. Le verrouillage {@code
 * FOR UPDATE SKIP LOCKED} permet plusieurs instances du service sans double publication ; en cas
 * d'échec, le lot s'interrompt pour préserver l'ordre et sera retenté au passage suivant.
 */
public class OutboxRelay {

  private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);
  private static final int MAX_ERREUR = 500;

  private final JdbcTemplate jdbc;
  private final KafkaTemplate<String, String> kafka;
  private final TransactionTemplate tx;
  private final OutboxProperties properties;
  private final Clock clock;

  /**
   * @param jdbc accès JDBC
   * @param kafka producteur Kafka
   * @param tx gabarit transactionnel
   * @param properties paramètres
   * @param clock horloge
   */
  public OutboxRelay(
      JdbcTemplate jdbc,
      KafkaTemplate<String, String> kafka,
      TransactionTemplate tx,
      OutboxProperties properties,
      Clock clock) {
    this.jdbc = jdbc;
    this.kafka = kafka;
    this.tx = tx;
    this.properties = properties;
    this.clock = clock;
  }

  private record Ligne(UUID id, String topic, String cle, String payload) {}

  /** Publie un lot d'événements en attente. */
  @Scheduled(fixedDelayString = "${cse.outbox.interval:PT1S}")
  public void relayer() {
    tx.executeWithoutResult(status -> publierLot());
  }

  /** Purge les événements publiés au-delà de la durée de rétention. */
  @Scheduled(cron = "${cse.outbox.purge-cron:0 30 2 * * *}")
  public void purger() {
    Instant limite = Instant.now(clock).minus(properties.retention());
    int n =
        jdbc.update(
            "DELETE FROM outbox_event WHERE published_at IS NOT NULL AND published_at < ?",
            Timestamp.from(limite));
    LOG.info("Outbox : {} événements publiés purgés", n);
  }

  private void publierLot() {
    List<Ligne> lignes =
        jdbc.query(
            "SELECT id, topic, message_key, payload FROM outbox_event WHERE published_at IS NULL"
                + " ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED",
            (rs, i) ->
                new Ligne(
                    rs.getObject("id", UUID.class),
                    rs.getString("topic"),
                    rs.getString("message_key"),
                    rs.getString("payload")),
            properties.batchSize());
    for (Ligne ligne : lignes) {
      if (!envoyer(ligne)) {
        return;
      }
    }
  }

  private boolean envoyer(Ligne ligne) {
    try {
      kafka
          .send(ligne.topic(), ligne.cle(), ligne.payload())
          .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
      jdbc.update(
          "UPDATE outbox_event SET published_at = ? WHERE id = ?",
          Timestamp.from(Instant.now(clock)),
          ligne.id());
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return echec(ligne, e);
    } catch (ExecutionException | TimeoutException e) {
      return echec(ligne, e);
    }
  }

  private boolean echec(Ligne ligne, Exception e) {
    String message = String.valueOf(e.getMessage());
    LOG.warn("Outbox : publication différée de l'événement {} : {}", ligne.id(), message);
    jdbc.update(
        "UPDATE outbox_event SET attempts = attempts + 1, last_error = ? WHERE id = ?",
        message.length() > MAX_ERREUR ? message.substring(0, MAX_ERREUR) : message,
        ligne.id());
    return false;
  }
}
