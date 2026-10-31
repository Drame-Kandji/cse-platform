package sn.cse.chatbot;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.test.PostgresTestConfiguration;

/**
 * Bout en bout : PostgreSQL réel (Testcontainers), services aval simulés (WireMock), aucun LLM
 * (mode dégradé), disjoncteurs Resilience4j actifs. Contient le test de snapshot OpenAPI.
 */
@SpringBootTest(properties = {"cse.chatbot.llm.base-url=", "cse.chatbot.llm.model="})
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class ChatbotIntegrationTest {

  private static final WireMockServer WM = new WireMockServer(wireMockConfig().dynamicPort());
  private static final UUID RM = UUID.fromString("00000000-0000-0000-0000-000000000201");
  private static final UUID DG = UUID.fromString("00000000-0000-0000-0000-000000000501");

  static {
    WM.start();
  }

  @DynamicPropertySource
  static void services(DynamicPropertyRegistry r) {
    for (String s : new String[] {"suivi", "besoins", "achats", "fournisseurs"}) {
      r.add("cse.chatbot.services." + s, WM::baseUrl);
    }
  }

  @AfterAll
  static void arreter() {
    WM.stop();
  }

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;

  @Test
  void should_answer_from_real_data_store_history_and_isolate_users() throws Exception {
    WM.stubFor(
        WireMock.get(urlPathEqualTo("/api/materiels"))
            .willReturn(
                okJson(
                    """
        {"content":[{"id":"m1","code":"NIV-002","designation":"Niveleuse CAT 140K","categorie":"MATERIEL_ROULANT",
          "etat":"AFFECTE","chantierActuelNom":"Contournement de Thiès RN2","latitude":14.791,"longitude":-16.9256}]}""")));
    WM.stubFor(
        WireMock.get(urlPathEqualTo("/api/besoins")).willReturn(aResponse().withStatus(503)));

    String brut =
        mvc.perform(
                post("/api/chatbot/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"question\":\"Où se trouve la niveleuse NIV-002 ?\"}")
                    .with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.intention").value("LOCALISER_ENGIN"))
            .andExpect(jsonPath("$.modeDegrade").value(true))
            .andExpect(
                jsonPath("$.reponse")
                    .value(containsString("sur le chantier **Contournement de Thiès RN2**")))
            .andExpect(jsonPath("$.sources[0].lien").value("/materiels/m1"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String conversationId = mapper.readTree(brut).path("conversationId").asText();
    WM.verify(
        getRequestedFor(urlPathEqualTo("/api/materiels"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withQueryParam("q", equalTo("NIV-002")));

    // Service aval en panne : réponse dégradée sans erreur (repli Resilience4j), même conversation.
    mvc.perform(
            post("/api/chatbot/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"question\":\"Quels besoins sont en attente ?\",\"conversationId\":\""
                        + conversationId
                        + "\"}")
                .with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conversationId").value(conversationId))
        .andExpect(jsonPath("$.reponse").value(containsString("momentanément indisponible")));

    mvc.perform(get("/api/chatbot/historique").with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].nombreMessages").value(4))
        .andExpect(jsonPath("$.content[0].titre").value("Où se trouve la niveleuse NIV-002 ?"));
    mvc.perform(
            get("/api/chatbot/historique/{id}", conversationId)
                .with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messages.length()").value(4))
        .andExpect(jsonPath("$.messages[1].sources[0].type").value("MATERIEL"));

    mvc.perform(
            get("/api/chatbot/historique/{id}", conversationId)
                .with(CseJwt.as(DG, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isNotFound());
    mvc.perform(
            delete("/api/chatbot/historique/{id}", conversationId)
                .with(CseJwt.as(DG, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/chatbot/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"question\":\"Et ensuite ?\",\"conversationId\":\"" + conversationId + "\"}")
                .with(CseJwt.as(DG, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/chatbot/historique").with(CseJwt.as(DG, Roles.DIRECTION_GENERALE)))
        .andExpect(jsonPath("$.totalElements").value(0));

    mvc.perform(
            delete("/api/chatbot/historique/{id}", conversationId)
                .with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isNoContent());
    mvc.perform(
            get("/api/chatbot/historique/{id}", conversationId)
                .with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isNotFound());

    mvc.perform(
            post("/api/chatbot/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"" + "a".repeat(1001) + "\"}")
                .with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("QUESTION_TROP_LONGUE"));
    mvc.perform(get("/api/chatbot/suggestions").with(CseJwt.chef(RM)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("Quels engins sont affectés à mon chantier ?"));
  }

  @Test
  void should_match_openapi_snapshot() throws Exception {
    JsonNode actuel =
        mapper.readTree(
            mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    ((ObjectNode) actuel).remove("servers");
    Path snapshot = Path.of("src/test/resources/openapi.json");
    if (!Files.exists(snapshot) || Boolean.getBoolean("openapi.regenerer")) {
      Files.writeString(
          snapshot,
          mapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(actuel));
    }
    assertThat(actuel).isEqualTo(mapper.readTree(Files.readString(snapshot)));
    assertThat(actuel.path("paths").has("/api/chatbot/query")).isTrue();
  }
}
