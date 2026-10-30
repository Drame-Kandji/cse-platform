package sn.cse.auth.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import sn.cse.auth.application.port.in.SynchroniserUtilisateursUseCase;
import sn.cse.auth.application.query.ResultatSynchronisation;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.test.PostgresTestConfiguration;

/**
 * Bout en bout : PostgreSQL réel (Testcontainers, Flyway, SINGLE_TABLE), Keycloak simulé
 * (WireMock), outbox, sécurité, contrat OpenAPI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class MsAuthIntegrationTest {

  private static final String ADMIN_API = "/admin/realms/cse";
  private static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000601");
  private static final UUID CHEF = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID DAF = UUID.fromString("00000000-0000-0000-0000-000000000401");
  private static final UUID NOUVEAU = UUID.fromString("00000000-0000-0000-0000-000000000999");
  private static final String CHANTIER = "c0000000-0000-0000-0000-000000000001";
  private static final Path SNAPSHOT = Path.of("src/test/resources/openapi.json");

  private static final WireMockServer KEYCLOAK = new WireMockServer(wireMockConfig().dynamicPort());

  static {
    KEYCLOAK.start();
  }

  @DynamicPropertySource
  static void proprietes(DynamicPropertyRegistry registry) {
    registry.add("cse.auth.keycloak.url", KEYCLOAK::baseUrl);
    registry.add("cse.auth.keycloak.client-secret", () -> "secret-test");
  }

  @AfterAll
  static void arreter() {
    KEYCLOAK.stop();
  }

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper mapper;
  @Autowired private SynchroniserUtilisateursUseCase synchronisation;

  @BeforeEach
  void initialiser() {
    jdbc.update("DELETE FROM utilisateurs");
    jdbc.update("DELETE FROM outbox_event");
    KEYCLOAK.resetAll();
    KEYCLOAK.stubFor(
        post(urlEqualTo("/realms/cse/protocol/openid-connect/token"))
            .willReturn(okJson("{\"access_token\":\"jeton\",\"expires_in\":300}")));
  }

  @Test
  void should_importer_les_comptes_keycloak_de_facon_idempotente_when_synchronisation() {
    KEYCLOAK.stubFor(
        get(urlPathMatching(ADMIN_API + "/roles/[A-Z_]+/users")).willReturn(okJson("[]")));
    KEYCLOAK.stubFor(
        get(urlPathEqualTo(ADMIN_API + "/roles/CHEF_CHANTIER/users"))
            .willReturn(okJson("[{\"id\":\"" + CHEF + "\"}]")));
    KEYCLOAK.stubFor(
        get(urlPathEqualTo(ADMIN_API + "/roles/DAF/users"))
            .willReturn(okJson("[{\"id\":\"" + DAF + "\"}]")));
    compteKeycloak(
        CHEF,
        "chef.chantier@cse.sn",
        "Mamadou",
        "Diop",
        "{\"chantier_ids\":[\"" + CHANTIER + "\"]}");
    compteKeycloak(DAF, "daf@cse.sn", "Ousmane", "Ba", "{\"budget_gere\":[\"5000000000\"]}");

    assertThat(synchronisation.synchroniser()).isEqualTo(new ResultatSynchronisation(2, 0, 0, 0));
    assertThat(synchronisation.synchroniser()).isEqualTo(new ResultatSynchronisation(0, 0, 0, 2));

    assertThat(
            jdbc.queryForObject(
                "SELECT role || ':' || array_to_string(chantier_ids, ',') FROM utilisateurs WHERE id = ?",
                String.class,
                CHEF))
        .isEqualTo("CHEF_CHANTIER:" + CHANTIER);
    assertThat(
            jdbc.queryForObject(
                "SELECT budget_gere FROM utilisateurs WHERE id = ?", Long.class, DAF))
        .isEqualTo(5_000_000_000L);
    assertThat(evenements("UTILISATEUR_SYNCHRONISE")).isEqualTo(2);
    assertThat(evenements("AUDIT")).isEqualTo(2);
  }

  @Test
  void should_creer_puis_changer_de_role_when_administration() throws Exception {
    KEYCLOAK.stubFor(
        post(urlEqualTo(ADMIN_API + "/users"))
            .willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader("Location", ADMIN_API + "/users/" + NOUVEAU)));
    KEYCLOAK.stubFor(
        get(urlEqualTo(ADMIN_API + "/users/" + NOUVEAU + "/role-mappings/realm"))
            .willReturn(okJson("[]")));
    KEYCLOAK.stubFor(
        get(urlPathMatching(ADMIN_API + "/roles/[A-Z_]+"))
            .willReturn(okJson("{\"id\":\"r\",\"name\":\"X\"}")));
    KEYCLOAK.stubFor(
        post(urlEqualTo(ADMIN_API + "/users/" + NOUVEAU + "/role-mappings/realm"))
            .willReturn(aResponse().withStatus(204)));
    KEYCLOAK.stubFor(
        get(urlEqualTo(ADMIN_API + "/users/" + NOUVEAU))
            .willReturn(okJson("{\"id\":\"" + NOUVEAU + "\"}")));
    KEYCLOAK.stubFor(
        put(urlEqualTo(ADMIN_API + "/users/" + NOUVEAU)).willReturn(aResponse().withStatus(204)));

    mvc.perform(
            MockMvcRequestBuilders.post("/api/admin/utilisateurs")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"awa.diallo@cse.sn\",\"prenom\":\"Awa\",\"nom\":\"Diallo\","
                        + "\"role\":\"CHEF_CHANTIER\",\"motDePasseTemporaire\":\"Temp@2026!\","
                        + "\"chantierIds\":[\""
                        + CHANTIER
                        + "\"]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(NOUVEAU.toString()))
        .andExpect(jsonPath("$.chantierIds[0]").value(CHANTIER));
    assertThat(evenements("UTILISATEUR_CREE")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT payload FROM outbox_event WHERE event_type = 'UTILISATEUR_CREE'",
                String.class))
        .contains("\"id\":\"" + ADMIN + "\"")
        .contains("\"chantierIds\":[\"" + CHANTIER + "\"]");

    mvc.perform(
            MockMvcRequestBuilders.put("/api/admin/utilisateurs/" + NOUVEAU)
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"awa.diallo@cse.sn\",\"prenom\":\"Awa\",\"nom\":\"Diallo\","
                        + "\"role\":\"RESPONSABLE_ACHATS\",\"seuilDelegation\":100000000}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("RESPONSABLE_ACHATS"))
        .andExpect(jsonPath("$.seuilDelegation").value(100000000))
        .andExpect(jsonPath("$.chantierIds").doesNotExist());
    assertThat(
            jdbc.queryForObject(
                "SELECT role || ':' || coalesce(array_length(chantier_ids, 1), 0) FROM utilisateurs WHERE id = ?",
                String.class,
                NOUVEAU))
        .isEqualTo("RESPONSABLE_ACHATS:0");

    mvc.perform(
            MockMvcRequestBuilders.get(
                    "/api/admin/utilisateurs?role=RESPONSABLE_ACHATS&q=DIAL&sort=createdAt,desc")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].email").value("awa.diallo@cse.sn"));
    mvc.perform(
            MockMvcRequestBuilders.get("/api/admin/roles")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code == 'RESPONSABLE_ACHATS')].nombreUtilisateurs").value(1));
  }

  @Test
  void should_annuler_et_supprimer_le_compte_keycloak_when_attribution_du_role_echoue()
      throws Exception {
    KEYCLOAK.stubFor(
        post(urlEqualTo(ADMIN_API + "/users"))
            .willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader("Location", ADMIN_API + "/users/" + NOUVEAU)));
    KEYCLOAK.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));
    KEYCLOAK.stubFor(
        delete(urlEqualTo(ADMIN_API + "/users/" + NOUVEAU))
            .willReturn(aResponse().withStatus(204)));

    mvc.perform(
            MockMvcRequestBuilders.post("/api/admin/utilisateurs")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"x@cse.sn\",\"prenom\":\"X\",\"nom\":\"Y\",\"role\":\"DAF\","
                        + "\"motDePasseTemporaire\":\"Temp@2026!\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("IDENTITE_INDISPONIBLE"));

    KEYCLOAK.verify(
        com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor(
            urlEqualTo(ADMIN_API + "/users/" + NOUVEAU)));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM utilisateurs", Long.class)).isZero();
    assertThat(evenements("UTILISATEUR_CREE")).isZero();
  }

  @Test
  void should_provisionner_mon_profil_depuis_le_jeton_when_premier_appel() throws Exception {
    mvc.perform(
            MockMvcRequestBuilders.get("/api/me")
                .with(
                    CseJwt.avecClaims(
                        CHEF,
                        Map.of(
                            "given_name", "Mamadou",
                            "family_name", "Diop",
                            "email", "chef.chantier@cse.sn",
                            "chantier_ids", List.of(CHANTIER)),
                        Roles.CHEF_CHANTIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.prenom").value("Mamadou"))
        .andExpect(jsonPath("$.role").value("CHEF_CHANTIER"))
        .andExpect(jsonPath("$.chantierIds[0]").value(CHANTIER));
    mvc.perform(MockMvcRequestBuilders.get("/api/me").with(CseJwt.as(CHEF, Roles.CHEF_CHANTIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nom").value("Diop"));
    assertThat(evenements("UTILISATEUR_SYNCHRONISE")).isEqualTo(1);
  }

  @Test
  void should_respecter_le_contrat_openapi_when_documentation_generee() throws Exception {
    String brut =
        mvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    ObjectNode document = (ObjectNode) mapper.readTree(brut);
    document.remove("servers");
    String actuel = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
    if (Boolean.getBoolean("openapi.snapshot.update") || !Files.exists(SNAPSHOT)) {
      Files.writeString(SNAPSHOT, actuel + System.lineSeparator(), StandardCharsets.UTF_8);
    }
    JSONAssert.assertEquals(
        Files.readString(SNAPSHOT, StandardCharsets.UTF_8), actuel, JSONCompareMode.NON_EXTENSIBLE);
  }

  private void compteKeycloak(UUID id, String email, String prenom, String nom, String attributs) {
    KEYCLOAK.stubFor(
        get(urlEqualTo(ADMIN_API + "/users/" + id))
            .willReturn(
                okJson(
                    "{\"id\":\""
                        + id
                        + "\",\"email\":\""
                        + email
                        + "\",\"firstName\":\""
                        + prenom
                        + "\",\"lastName\":\""
                        + nom
                        + "\",\"enabled\":true,\"attributes\":"
                        + attributs
                        + "}")));
    KEYCLOAK.stubFor(
        get(urlEqualTo(ADMIN_API + "/users/" + id + "/credentials")).willReturn(okJson("[]")));
  }

  private long evenements(String type) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM outbox_event WHERE event_type = ?", Long.class, type);
  }
}
