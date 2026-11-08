package sn.cse.besoins.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import sn.cse.besoins.application.port.out.BesoinRepository;
import sn.cse.besoins.infrastructure.client.SuiviRestAdapter;
import sn.cse.besoins.infrastructure.messaging.SuiviEventsListener;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.test.PostgresTestConfiguration;

/**
 * Bout en bout sur PostgreSQL (Testcontainers) avec ms-suivi simulé par WireMock : soumission →
 * validation → AFFECTE_DIRECT / EN_ACHAT, panne de ms-suivi puis relance, outbox, consommateur
 * Kafka idempotent, données de démonstration et instantané du contrat OpenAPI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
class BesoinsIntegrationTest {

  private static final WireMockServer SUIVI = new WireMockServer(options().dynamicPort());

  static {
    SUIVI.start();
  }

  private static final UUID CHANTIER_1 = UUID.fromString("c0000000-0000-0000-0000-000000000001");
  private static final UUID CHANTIER_3 = UUID.fromString("c0000000-0000-0000-0000-000000000003");
  private static final UUID CHEF = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID RM = UUID.fromString("00000000-0000-0000-0000-000000000201");
  private static final Path SNAPSHOT = Path.of("src/test/resources/openapi.json");

  @DynamicPropertySource
  static void proprietes(DynamicPropertyRegistry registry) {
    registry.add("cse.besoins.suivi.base-url", SUIVI::baseUrl);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired SuiviEventsListener listener;
  @Autowired CircuitBreakerRegistry disjoncteurs;
  @Autowired BesoinRepository repository;
  @Autowired Clock clock;

  @Value("${spring.application.name}")
  String service;

  @AfterAll
  static void arreter() {
    SUIVI.stop();
  }

  @BeforeEach
  void reinitialiser() {
    SUIVI.resetAll();
    disjoncteurs.circuitBreaker(SuiviRestAdapter.INSTANCE).reset();
    SUIVI.stubFor(
        WireMock.get(urlPathMatching("/api/chantiers/.*"))
            .willReturn(
                okJson(
                    "{\"id\":\""
                        + CHANTIER_1
                        + "\",\"nom\":\"Extension autoroute Dakar-AIBD\","
                        + "\"statut\":\"EN_COURS\"}")));
  }

  // ---------------------------------------------------------------- scénarios de bout en bout

  @Test
  void should_soumettre_valider_et_affecter_directement_puis_refuser_toute_modification_R01_R05()
      throws Exception {
    UUID m1 = UUID.randomUUID();
    UUID m2 = UUID.randomUUID();
    disponibilite(m1, m2, UUID.randomUUID());
    SUIVI.stubFor(
        WireMock.post(urlPathMatching("/api/materiels/.*/affecter"))
            .willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"" + UUID.randomUUID() + "\"}")));

    JsonNode cree = soumettre(2, "NORMAL", null);
    String id = cree.get("id").asText();
    int annee = LocalDate.now(clock).getYear();
    assertThat(cree.get("reference").asText()).matches("BES-" + annee + "-\\d{4}");
    assertThat(cree.get("statut").asText()).isEqualTo("EN_ATTENTE");

    // R01 : le chef ne peut pas valider.
    mvc.perform(patch("/api/besoins/{id}/valider", id).with(chef()))
        .andExpect(status().isForbidden());

    JsonNode valide =
        lire(
            mvc.perform(
                    json(
                            patch("/api/besoins/{id}/valider", id),
                            "{\"commentaire\":\"Conforme\",\"materielIds\":[\"" + m2 + "\"]}")
                        .with(rm()))
                .andExpect(status().isOk()));
    assertThat(valide.get("statut").asText()).isEqualTo("AFFECTE_DIRECT");
    assertThat(valide.get("orientationEnAttente").asBoolean()).isFalse();
    assertThat(valide.get("validateurId").asText()).isEqualTo(RM.toString());
    assertThat(valide.get("materielIdsAffectes"))
        .extracting(JsonNode::asText)
        .containsExactly(m2.toString(), m1.toString());
    // JWT de l'utilisateur relayé à ms-suivi.
    SUIVI.verify(
        2,
        postRequestedFor(urlPathMatching("/api/materiels/.*/affecter"))
            .withHeader("Authorization", equalTo("Bearer token")));

    assertThat(evenements(id))
        .containsExactly("BESOIN_SOUMIS", "BESOIN_VALIDE", "BESOIN_AFFECTE_DIRECT");
    assertThat(audits(id)).isEqualTo(3);

    mvc.perform(get("/api/besoins/{id}/historique", id).with(chef()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].action").value("SOUMISSION"))
        .andExpect(jsonPath("$[1].action").value("VALIDATION"))
        .andExpect(jsonPath("$[1].commentaire").value("Conforme"))
        .andExpect(jsonPath("$[2].action").value("AFFECTATION_DIRECTE"))
        .andExpect(jsonPath("$[2].statut").value("AFFECTE_DIRECT"));

    // R05 : plus modifiable.
    mvc.perform(
            json(
                    put("/api/besoins/{id}", id),
                    corps(2, "NORMAL", null, valide.get("version").asLong()))
                .with(chef()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("BESOIN_NON_MODIFIABLE"));

    // Consommateur Kafka : MATERIEL_AFFECTE historisé une seule fois (idempotence).
    String evenement = materielAffecte(UUID.fromString(id));
    listener.recevoir(evenement);
    listener.recevoir(evenement);
    mvc.perform(get("/api/besoins/{id}/historique", id).with(rm()))
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(
            jsonPath("$[?(@.action == 'MATERIEL_AFFECTE' && @.auteurNom == 'Ibrahima Fall')]")
                .exists());
  }

  @Test
  void should_soumettre_valider_et_orienter_vers_les_achats() throws Exception {
    disponibilite();

    String id =
        soumettre(1, "URGENT", "Grue indispensable à la pose des poutres").get("id").asText();
    mvc.perform(patch("/api/besoins/{id}/valider", id).with(rm()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("EN_ACHAT"))
        .andExpect(jsonPath("$.orientationEnAttente").value(false));

    assertThat(evenements(id)).containsExactly("BESOIN_SOUMIS", "BESOIN_VALIDE", "BESOIN_EN_ACHAT");
    JsonNode payload =
        mapper.readTree(
            jdbc.queryForObject(
                "SELECT payload FROM outbox_event WHERE message_key = ? AND event_type = 'BESOIN_EN_ACHAT'",
                String.class,
                id));
    assertThat(payload.at("/payload/statut").asText()).isEqualTo("EN_ACHAT");
    assertThat(payload.at("/payload/priorite").asText()).isEqualTo("URGENT");
    assertThat(payload.at("/actor/id").asText()).isEqualTo(RM.toString());
    assertThat(payload.get("source").asText()).isEqualTo(service);
  }

  @Test
  void should_rester_valide_quand_ms_suivi_est_indisponible_puis_relancer_l_orientation()
      throws Exception {
    SUIVI.stubFor(
        WireMock.get(urlPathEqualTo("/api/materiels/disponibilite"))
            .willReturn(aResponse().withStatus(503)));

    String id = soumettre(1, "NORMAL", null).get("id").asText();
    mvc.perform(patch("/api/besoins/{id}/valider", id).with(rm()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("VALIDE"))
        .andExpect(jsonPath("$.orientationEnAttente").value(true));

    UUID materiel = UUID.randomUUID();
    // ms-suivi rétabli et disjoncteur refermé (délai d'ouverture écoulé).
    SUIVI.resetAll();
    disjoncteurs.circuitBreaker(SuiviRestAdapter.INSTANCE).reset();
    disponibilite(materiel);
    SUIVI.stubFor(
        WireMock.post(urlPathEqualTo("/api/materiels/" + materiel + "/affecter"))
            .willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{}")));

    mvc.perform(post("/api/besoins/{id}/orienter", id).with(rm()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("AFFECTE_DIRECT"))
        .andExpect(jsonPath("$.materielIdsAffectes[0]").value(materiel.toString()));
    mvc.perform(post("/api/besoins/{id}/orienter", id).with(rm()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ORIENTATION_NON_REQUISE"));
    mvc.perform(get("/api/besoins/{id}/historique", id).with(rm()))
        .andExpect(jsonPath("$[2].action").value("ORIENTATION_EN_ATTENTE"));
  }

  @Test
  void should_appliquer_les_regles_de_soumission_et_de_visibilite_R08() throws Exception {
    mvc.perform(json(post("/api/besoins"), corps(1, "CRITIQUE", null, null)).with(chef()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("JUSTIFICATION_OBLIGATOIRE"));
    mvc.perform(
            json(
                    post("/api/besoins"),
                    corps(1, "NORMAL", null, null)
                        .replace(CHANTIER_1.toString(), CHANTIER_3.toString()))
                .with(chef()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CHANTIER_NON_ATTRIBUE"));

    String id = soumettre(1, "NORMAL", null).get("id").asText();
    mvc.perform(json(patch("/api/besoins/{id}/rejeter", id), "{\"motif\":\"  \"}").with(rm()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("MOTIF_REJET_OBLIGATOIRE"));
    mvc.perform(json(put("/api/besoins/{id}", id), corps(3, "NORMAL", null, 99L)).with(chef()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLIT_CONCURRENCE"));
    mvc.perform(json(put("/api/besoins/{id}", id), corps(3, "NORMAL", null, 0L)).with(chef()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantite").value(3))
        .andExpect(jsonPath("$.version").value(1));

    var autreChef = CseJwt.chef(UUID.randomUUID(), CHANTIER_3);
    mvc.perform(get("/api/besoins/{id}", id).with(autreChef))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_CHANTIER_REFUSE"));
    mvc.perform(get("/api/besoins").param("q", "pelle").with(autreChef))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id == '" + id + "')]").isEmpty());
    mvc.perform(
            get("/api/besoins")
                .param("chantierId", CHANTIER_1.toString())
                .param("sort", "priorite,desc")
                .with(chef()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id == '" + id + "')]").exists());
    mvc.perform(get("/api/besoins/{id}", UUID.randomUUID()).with(rm()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BESOIN_INTROUVABLE"));
  }

  // ---------------------------------------------------------------- contrat OpenAPI

  @Test
  void should_respecter_l_instantane_du_contrat_openapi() throws Exception {
    String brut =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    ObjectNode actuel = (ObjectNode) mapper.readTree(brut);
    actuel.remove("servers");
    String formate = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actuel) + "\n";
    if (!Files.exists(SNAPSHOT) || Boolean.getBoolean("openapi.snapshot.update")) {
      // Régénération volontaire : supprimer le fichier ou lancer avec
      // -Dopenapi.snapshot.update=true.
      Files.writeString(SNAPSHOT, formate, StandardCharsets.UTF_8);
    }
    JsonNode attendu = mapper.readTree(Files.readString(SNAPSHOT, StandardCharsets.UTF_8));
    assertThat(actuel).isEqualTo(attendu);
    assertThat(
            actuel.at("/components/schemas/Besoin/properties/orientationEnAttente").isMissingNode())
        .isFalse();
    assertThat(actuel.at("/paths/~1api~1besoins~1{id}~1valider/patch").isMissingNode()).isFalse();
  }

  // ---------------------------------------------------------------- outils

  private static RequestPostProcessor chef() {
    return CseJwt.chef(CHEF, CHANTIER_1, UUID.fromString("c0000000-0000-0000-0000-000000000002"));
  }

  private static RequestPostProcessor rm() {
    return CseJwt.avecClaims(RM, Map.of("name", "Ibrahima Fall"), Roles.RESPONSABLE_MATERIEL);
  }

  private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String corps) {
    return b.contentType(MediaType.APPLICATION_JSON).content(corps);
  }

  private String corps(int quantite, String priorite, String justification, Long version) {
    LocalDate date = LocalDate.now(clock).plusDays(10);
    return "{\"chantierId\":\""
        + CHANTIER_1
        + "\",\"designation\":\"Pelle hydraulique 20 t\","
        + "\"categorie\":\"MATERIEL_ROULANT\",\"quantite\":"
        + quantite
        + ",\"priorite\":\""
        + priorite
        + "\","
        + "\"dureeUtilisationJours\":30,\"dateBesoin\":\""
        + date
        + "\""
        + (justification == null ? "" : ",\"justification\":\"" + justification + "\"")
        + (version == null ? "" : ",\"version\":" + version)
        + "}";
  }

  private JsonNode soumettre(int quantite, String priorite, String justification) throws Exception {
    return lire(
        mvc.perform(
                json(post("/api/besoins"), corps(quantite, priorite, justification, null))
                    .with(chef()))
            .andExpect(status().isCreated()));
  }

  private JsonNode lire(org.springframework.test.web.servlet.ResultActions r) throws Exception {
    return mapper.readTree(r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
  }

  private void disponibilite(UUID... materiels) {
    StringBuilder liste = new StringBuilder();
    for (UUID m : materiels) {
      liste.append(liste.length() == 0 ? "" : ",").append("{\"id\":\"").append(m).append("\"}");
    }
    SUIVI.stubFor(
        WireMock.get(urlPathEqualTo("/api/materiels/disponibilite"))
            .willReturn(
                okJson(
                    "{\"disponible\":"
                        + (materiels.length > 0)
                        + ",\"quantiteDemandee\":1,"
                        + "\"quantiteDisponible\":"
                        + materiels.length
                        + ",\"materiels\":["
                        + liste
                        + "]}")));
  }

  private List<String> evenements(String id) {
    return jdbc.queryForList(
        "SELECT event_type FROM outbox_event WHERE message_key = ? AND topic = 'cse.besoins.events'"
            + " ORDER BY created_at, event_type DESC",
        String.class,
        id);
  }

  private int audits(String id) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM outbox_event WHERE message_key = ? AND topic = 'cse.audit.events'",
        Integer.class,
        id);
  }

  private String materielAffecte(UUID besoin) throws Exception {
    Map<String, Object> enveloppe =
        Map.of(
            "eventId", UUID.randomUUID().toString(),
            "type", "MATERIEL_AFFECTE",
            "version", 1,
            "occurredAt",
                Instant.now(clock)
                    .truncatedTo(ChronoUnit.SECONDS)
                    .atOffset(ZoneOffset.UTC)
                    .toString(),
            "source", "ms-suivi",
            "aggregateType", "Affectation",
            "aggregateId", UUID.randomUUID().toString(),
            "actor",
                Map.of(
                    "id",
                    RM.toString(),
                    "nom",
                    "Ibrahima Fall",
                    "roles",
                    List.of(Roles.RESPONSABLE_MATERIEL)),
            "payload",
                Map.of(
                    "affectationId", UUID.randomUUID().toString(),
                    "materielId", UUID.randomUUID().toString(),
                    "materielCode", "PEL-007",
                    "materielDesignation", "Pelle hydraulique 20 t",
                    "categorie", "MATERIEL_ROULANT",
                    "chantierId", CHANTIER_1.toString(),
                    "chantierNom", "Extension autoroute Dakar-AIBD",
                    "besoinId", besoin.toString(),
                    "dateDebut", LocalDate.now(clock).toString(),
                    "dateFinPrevue", LocalDate.now(clock).plusDays(30).toString()));
    return mapper.writeValueAsString(enveloppe);
  }
}
