package sn.cse.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.common.test.PostgresTestConfiguration;
import sn.cse.events.EventEnvelope;
import sn.cse.events.Topics;

/** Outbox et idempotence sur une vraie base PostgreSQL (Testcontainers). */
@SpringBootTest(
    classes = OutboxIntegrationTest.App.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "spring.application.name=ms-test",
      "cse.outbox.relay-enabled=false",
      "spring.flyway.locations=classpath:db/common"
    })
@Import(PostgresTestConfiguration.class)
class OutboxIntegrationTest {

  @SpringBootApplication
  static class App {}

  @Autowired OutboxWriter writer;
  @Autowired IdempotentEventHandler idempotence;
  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate tx;
  @Autowired ObjectMapper mapper;

  @BeforeEach
  void vider() {
    jdbc.update("DELETE FROM outbox_event");
    jdbc.update("DELETE FROM processed_event");
  }

  @Test
  void should_write_domain_event_and_audit_entry_in_same_transaction() throws Exception {
    UUID id = UUID.randomUUID();

    tx.executeWithoutResult(
        s ->
            writer.publier(
                Topics.BESOINS,
                "BESOIN_SOUMIS",
                "BesoinMateriel",
                id,
                Map.of("quantite", 2),
                "ok"));

    List<Map<String, Object>> lignes =
        jdbc.queryForList("SELECT topic, payload FROM outbox_event ORDER BY topic");
    assertThat(lignes)
        .extracting(l -> l.get("topic"))
        .containsExactly(Topics.AUDIT, Topics.BESOINS);
    EventEnvelope env =
        mapper.readValue((String) lignes.get(1).get("payload"), EventEnvelope.class);
    assertThat(env.aggregateId()).isEqualTo(id.toString());
    assertThat(env.source()).isEqualTo("ms-test");
    assertThat(env.payload().get("quantite").asInt()).isEqualTo(2);
  }

  @Test
  void should_not_persist_events_when_transaction_rolls_back() {
    assertThatThrownBy(
            () ->
                tx.executeWithoutResult(
                    s -> {
                      writer.publier(Topics.SUIVI, "X", "Materiel", UUID.randomUUID(), Map.of());
                      throw new IllegalStateException("échec métier");
                    }))
        .isInstanceOf(IllegalStateException.class);

    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_event", Integer.class)).isZero();
  }

  @Test
  void should_refuse_publication_outside_transaction() {
    assertThatThrownBy(() -> writer.publier(Topics.SUIVI, "X", "Materiel", "1", Map.of()))
        .isInstanceOf(IllegalTransactionStateException.class);
  }

  @Test
  void should_process_event_only_once_per_consumer() {
    UUID eventId = UUID.randomUUID();
    AtomicInteger compteur = new AtomicInteger();

    boolean premier = idempotence.traiterUneFois(eventId, "reporting", compteur::incrementAndGet);
    boolean second = idempotence.traiterUneFois(eventId, "reporting", compteur::incrementAndGet);
    boolean autre = idempotence.traiterUneFois(eventId, "notification", compteur::incrementAndGet);

    assertThat(premier).isTrue();
    assertThat(second).isFalse();
    assertThat(autre).isTrue();
    assertThat(compteur).hasValue(2);
  }
}
