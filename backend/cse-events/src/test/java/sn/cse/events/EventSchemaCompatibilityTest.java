package sn.cse.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sn.cse.events.payload.BesoinPayload;
import sn.cse.events.payload.ContratPayload;
import sn.cse.events.payload.FacturePayload;

/**
 * Vérifie que les exemples JSON committés restent lisibles par les records courants : une rupture
 * de schéma (champ renommé, type changé) fait échouer ce test.
 */
class EventSchemaCompatibilityTest {

  private final ObjectMapper mapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private EventEnvelope lire(String nom) throws IOException {
    try (InputStream in = getClass().getResourceAsStream("/examples/" + nom + ".json")) {
      return mapper.readValue(in, EventEnvelope.class);
    }
  }

  @Test
  void should_read_besoin_payload_when_example_is_committed() throws IOException {
    EventEnvelope envelope = lire(EventTypes.BESOIN_EN_ACHAT);
    BesoinPayload payload = envelope.payloadAs(BesoinPayload.class, mapper);

    assertThat(envelope.version()).isEqualTo(EventEnvelope.CURRENT_VERSION);
    assertThat(payload.reference()).isEqualTo("BES-2026-0001");
    assertThat(payload.quantite()).isEqualTo(2);
    assertThat(payload.materielIds()).isEmpty();
  }

  @Test
  void should_default_actor_to_system_when_absent() throws IOException {
    EventEnvelope envelope = lire(EventTypes.LIVRAISON_EFFECTUEE);

    assertThat(envelope.actor()).isEqualTo(Actor.SYSTEME);
    assertThat(envelope.payloadAs(ContratPayload.class, mapper).montant())
        .isEqualByComparingTo(new BigDecimal("245000000"));
  }

  @Test
  void should_round_trip_envelope_without_loss() throws IOException {
    EventEnvelope original = lire(EventTypes.BESOIN_EN_ACHAT);
    EventEnvelope relu = mapper.readValue(mapper.writeValueAsString(original), EventEnvelope.class);

    assertThat(relu).isEqualTo(original);
  }

  @Test
  void should_read_facture_business_dates_when_present() throws IOException {
    FacturePayload payload = lire(EventTypes.FACTURE_PAYEE).payloadAs(FacturePayload.class, mapper);

    assertThat(payload.dateEmission()).isEqualTo(LocalDate.of(2026, 9, 5));
    assertThat(payload.dateMandatement()).isEqualTo(LocalDate.of(2026, 9, 20));
    assertThat(payload.datePaiement()).isEqualTo(LocalDate.of(2026, 10, 2));
  }

  @Test
  void should_read_facture_without_business_dates_when_event_predates_them() throws IOException {
    FacturePayload payload =
        lire("FACTURE_MANDATEE_V1_SANS_DATES").payloadAs(FacturePayload.class, mapper);

    assertThat(payload.statut()).isEqualTo("MANDATEE");
    assertThat(payload.dateEcheance()).isEqualTo(LocalDate.of(2026, 10, 5));
    assertThat(payload.dateEmission()).isNull();
    assertThat(payload.dateMandatement()).isNull();
    assertThat(payload.datePaiement()).isNull();
  }
}
