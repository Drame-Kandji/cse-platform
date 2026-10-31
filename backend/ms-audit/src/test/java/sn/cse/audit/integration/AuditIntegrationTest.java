package sn.cse.audit.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.audit.infrastructure.messaging.AuditEventListener;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.test.PostgresTestConfiguration;
import sn.cse.events.Actor;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.payload.AuditPayload;

/**
 * Bout en bout sur PostgreSQL réel : consommation idempotente, immuabilité garantie par
 * déclencheur, API sécurisée et instantané du contrat OpenAPI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class AuditIntegrationTest {

  private static final Path SNAPSHOT = Path.of("src/test/resources/openapi.json");

  @Autowired private AuditEventListener listener;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper mapper;
  @Autowired private MockMvc mvc;

  private String message(UUID eventId, String action, String type) throws Exception {
    EventEnvelope env =
        new EventEnvelope(
            eventId,
            type,
            1,
            Instant.now(),
            "ms-besoins",
            "BesoinMateriel",
            "b-" + action,
            "corr-42",
            new Actor(
                "00000000-0000-0000-0000-000000000201",
                "Ibrahima Fall",
                List.of("RESPONSABLE_MATERIEL")),
            mapper.valueToTree(
                new AuditPayload(
                    "ms-besoins", action, "BesoinMateriel", "b-" + action, "Résumé " + action)));
    return mapper.writeValueAsString(env);
  }

  @Test
  void should_journaliser_une_fois_et_refuser_toute_modification() throws Exception {
    UUID eventId = UUID.randomUUID();
    String msg = message(eventId, "BESOIN_VALIDE_IT", EventTypes.AUDIT);

    listener.recevoir(msg);
    listener.recevoir(msg);
    listener.recevoir(message(UUID.randomUUID(), "IGNORE", EventTypes.BESOIN_VALIDE));

    Integer n =
        jdbc.queryForObject(
            "SELECT count(*) FROM audit_log WHERE event_id = ?", Integer.class, eventId);
    assertThat(n).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action = 'IGNORE'", Integer.class))
        .isZero();

    assertThatThrownBy(
            () -> jdbc.update("UPDATE audit_log SET resume = 'x' WHERE event_id = ?", eventId))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("immuable");
    assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_log WHERE event_id = ?", eventId))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("immuable");
    assertThatThrownBy(() -> jdbc.execute("TRUNCATE audit_log"))
        .isInstanceOf(DataAccessException.class);

    mvc.perform(
            get("/api/audit")
                .param("action", "BESOIN_VALIDE_IT")
                .param("q", "résumé")
                .with(CseJwt.as(UUID.randomUUID(), Roles.ADMIN_SYSTEME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].utilisateurNom").value("Ibrahima Fall"))
        .andExpect(jsonPath("$.content[0].correlationId").value("corr-42"))
        .andExpect(jsonPath("$.content[0].roles[0]").value("RESPONSABLE_MATERIEL"));

    mvc.perform(
            get("/api/audit/statistiques").with(CseJwt.as(UUID.randomUUID(), Roles.ADMIN_SYSTEME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parService[0].service").value("ms-besoins"));
  }

  @Test
  void should_envoyer_en_dlt_sans_rejeu_when_message_illisible() {
    assertThatThrownBy(() -> listener.recevoir("{pas du json"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_respecter_instantane_openapi() throws Exception {
    String brut =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode actuel = mapper.readTree(brut);
    if (Boolean.getBoolean("openapi.regenerer")) {
      Files.writeString(
          SNAPSHOT, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actuel) + "\n");
    }
    assertThat(SNAPSHOT)
        .as("Instantané OpenAPI absent : lancer une fois avec -Dopenapi.regenerer=true")
        .exists();
    assertThat(actuel)
        .as(
            "Le contrat OpenAPI a changé : régénérer volontairement src/test/resources/openapi.json")
        .isEqualTo(mapper.readTree(Files.readString(SNAPSHOT)));
  }
}
