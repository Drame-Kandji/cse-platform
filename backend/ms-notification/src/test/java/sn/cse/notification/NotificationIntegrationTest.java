package sn.cse.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.test.PostgresTestConfiguration;
import sn.cse.events.Actor;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.payload.AffectationPayload;
import sn.cse.events.payload.BesoinPayload;
import sn.cse.events.payload.UtilisateurPayload;
import sn.cse.notification.application.port.out.MailPort;
import sn.cse.notification.infrastructure.messaging.AnnuaireListener;
import sn.cse.notification.infrastructure.messaging.EvenementsMetierListener;

/**
 * Bout en bout sur PostgreSQL réel : enveloppes Kafka injectées dans les listeners, lecture par
 * l'API REST, e-mail simulé. Contient aussi le test de non-régression du contrat OpenAPI.
 */
@SpringBootTest(
    properties = {
      "spring.kafka.listener.auto-startup=false",
      "spring.kafka.admin.auto-create=false",
      "cse.notification.sse.ping-interval=PT1H"
    })
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class NotificationIntegrationTest {

  private static final UUID CHEF = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID RM1 = UUID.fromString("00000000-0000-0000-0000-000000000201");
  private static final UUID RM2 = UUID.randomUUID();
  private static final UUID CHANTIER = UUID.fromString("c0000000-0000-0000-0000-000000000001");

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private EvenementsMetierListener metier;
  @Autowired private AnnuaireListener annuaire;
  @MockitoBean private MailPort mail;

  @Test
  void should_route_events_to_users_and_roles_with_per_user_read_state() throws Exception {
    annuaire.consommer(
        enveloppe(
            EventTypes.UTILISATEUR_CREE,
            UUID.randomUUID(),
            new UtilisateurPayload(
                CHEF,
                "chef.chantier@cse.sn",
                "Mamadou",
                "Diop",
                Roles.CHEF_CHANTIER,
                true,
                List.of(CHANTIER))));

    UUID eventId = UUID.randomUUID();
    String affectation =
        enveloppe(
            EventTypes.MATERIEL_AFFECTE,
            eventId,
            new AffectationPayload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PEL-001",
                "Pelle hydraulique 20 t",
                "MATERIEL_ROULANT",
                BigDecimal.valueOf(250000),
                CHANTIER,
                "Extension autoroute Dakar–AIBD",
                null,
                null,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 12, 15),
                null,
                "EN_ATTENTE_RECEPTION",
                null,
                null));
    metier.consommer(affectation);
    metier.consommer(affectation); // rejeu Kafka : ignoré (idempotence)

    mvc.perform(get("/api/notifications").with(CseJwt.chef(CHEF, CHANTIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].type").value("MATERIEL_AFFECTE"))
        .andExpect(
            jsonPath("$.content[0].titre")
                .value("Matériel PEL-001 affecté à Extension autoroute Dakar–AIBD"))
        .andExpect(
            jsonPath("$.content[0].message")
                .value(org.hamcrest.Matchers.containsString("du 02/11/2026 au 15/12/2026")))
        .andExpect(jsonPath("$.content[0].lue").value(false));
    verify(mail, times(1)).envoyer(argThat(m -> m.adresse().equals("chef.chantier@cse.sn")));

    String id =
        mapper
            .readTree(
                mvc.perform(get("/api/notifications").with(CseJwt.chef(CHEF, CHANTIER)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("content")
            .get(0)
            .path("id")
            .asText();
    mvc.perform(
            patch("/api/notifications/{id}/lue", id)
                .with(CseJwt.as(RM1, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isNotFound());
    mvc.perform(patch("/api/notifications/{id}/lue", id).with(CseJwt.chef(CHEF, CHANTIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lue").value(true));
    mvc.perform(patch("/api/notifications/{id}/lue", id).with(CseJwt.chef(CHEF, CHANTIER)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/notifications/non-lues/count").with(CseJwt.chef(CHEF, CHANTIER)))
        .andExpect(jsonPath("$.count").value(0));

    UUID besoinId = UUID.randomUUID();
    metier.consommer(
        enveloppe(
            EventTypes.BESOIN_SOUMIS,
            UUID.randomUUID(),
            new BesoinPayload(
                besoinId,
                "BES-2026-0012",
                CHANTIER,
                "Extension autoroute Dakar–AIBD",
                CHEF,
                "Mamadou Diop",
                "Niveleuse",
                "MATERIEL_ROULANT",
                1,
                null,
                "URGENT",
                "EN_ATTENTE",
                LocalDate.of(2026, 11, 10),
                30,
                null,
                List.of())));

    mvc.perform(
            get("/api/notifications?lue=false").with(CseJwt.as(RM1, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].lien").value("/besoins/" + besoinId))
        .andExpect(
            jsonPath("$.content[0].message")
                .value(org.hamcrest.Matchers.containsString("priorité urgente")));
    mvc.perform(
            post("/api/notifications/tout-lire").with(CseJwt.as(RM1, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isNoContent());
    mvc.perform(
            get("/api/notifications/non-lues/count")
                .with(CseJwt.as(RM1, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(jsonPath("$.count").value(0));
    // Notification de rôle : l'autre détenteur du rôle ne l'a pas encore lue.
    mvc.perform(
            get("/api/notifications/non-lues/count")
                .with(CseJwt.as(RM2, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(jsonPath("$.count").value(1));
    mvc.perform(get("/api/notifications?lue=true").with(CseJwt.as(RM1, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(jsonPath("$.content[0].lue").value(true));
    // BESOIN_SOUMIS n'envoie pas d'e-mail (règle email: false).
    verify(mail, times(1)).envoyer(any());
  }

  @Test
  void should_match_openapi_snapshot() throws Exception {
    String brut =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode actuel = mapper.readTree(brut);
    ((com.fasterxml.jackson.databind.node.ObjectNode) actuel).remove("servers");
    Path snapshot = Path.of("src/test/resources/openapi.json");
    if (!Files.exists(snapshot) || Boolean.getBoolean("openapi.regenerer")) {
      Files.writeString(
          snapshot,
          mapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(actuel));
    }
    assertThat(actuel).isEqualTo(mapper.readTree(Files.readString(snapshot)));
    assertThat(actuel.path("paths").has("/api/notifications/stream")).isTrue();
  }

  private String enveloppe(String type, UUID eventId, Object payload) throws Exception {
    return mapper.writeValueAsString(
        new EventEnvelope(
            eventId,
            type,
            1,
            Instant.parse("2026-10-30T09:15:00Z"),
            "test",
            "Agregat",
            UUID.randomUUID().toString(),
            null,
            new Actor(RM1.toString(), "Ibrahima Fall", List.of(Roles.RESPONSABLE_MATERIEL)),
            mapper.valueToTree(payload)));
  }
}
