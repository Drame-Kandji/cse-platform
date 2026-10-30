package sn.cse.common.messaging;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garantit qu'un événement n'est traité qu'une seule fois par consommateur (Kafka livre « au moins
 * une fois »). L'insertion dans {@code processed_event} et le traitement partagent la même
 * transaction : si le traitement échoue, la marque est annulée et l'événement sera rejoué.
 */
public class IdempotentEventHandler {

  private final JdbcTemplate jdbc;
  private final Clock clock;

  /**
   * @param jdbc accès JDBC
   * @param clock horloge
   */
  public IdempotentEventHandler(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * @param eventId identifiant de l'événement
   * @param consommateur nom logique du consommateur
   * @param traitement action à exécuter
   * @return vrai si le traitement a été exécuté, faux si l'événement était déjà traité
   */
  @Transactional
  public boolean traiterUneFois(UUID eventId, String consommateur, Runnable traitement) {
    int inseres =
        jdbc.update(
            "INSERT INTO processed_event (event_id, consumer, processed_at) VALUES (?, ?, ?)"
                + " ON CONFLICT DO NOTHING",
            eventId,
            consommateur,
            Timestamp.from(Instant.now(clock)));
    if (inseres == 0) {
      return false;
    }
    traitement.run();
    return true;
  }
}
