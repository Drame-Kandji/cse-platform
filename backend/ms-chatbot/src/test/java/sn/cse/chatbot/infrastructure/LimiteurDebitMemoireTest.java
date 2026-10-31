package sn.cse.chatbot.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.chatbot.infrastructure.ratelimit.LimiteurDebitMemoire;

class LimiteurDebitMemoireTest {

  /** Horloge manipulable. */
  static final class Horloge extends Clock {
    Instant maintenant = Instant.parse("2026-10-30T09:00:00Z");

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return maintenant;
    }
  }

  @Test
  void should_allow_capacity_then_refill_over_time_per_user() {
    Horloge horloge = new Horloge();
    LimiteurDebitMemoire limiteur =
        new LimiteurDebitMemoire(Proprietes.chatbot("http://x", 2, Duration.ofMinutes(1)), horloge);
    UUID a = UUID.randomUUID();
    assertThat(limiteur.autoriser(a)).isTrue();
    assertThat(limiteur.autoriser(a)).isTrue();
    assertThat(limiteur.autoriser(a)).isFalse();
    assertThat(limiteur.autoriser(UUID.randomUUID())).isTrue();
    horloge.maintenant = horloge.maintenant.plusSeconds(30);
    assertThat(limiteur.autoriser(a)).isTrue();
    assertThat(limiteur.autoriser(a)).isFalse();
  }
}
