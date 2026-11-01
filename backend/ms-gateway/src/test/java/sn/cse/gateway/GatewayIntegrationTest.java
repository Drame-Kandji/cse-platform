package sn.cse.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.support.RouteMetadataUtils;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import sn.cse.common.security.Roles;

/**
 * Passerelle de bout en bout : services simulés par WireMock (ms-audit pointe vers un port fermé
 * pour exercer le coupe-circuit), jetons RS256 de test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(JetonsDeTest.class)
class GatewayIntegrationTest {

  private static final String SUB = "00000000-0000-0000-0000-000000000101";
  private static final String PORT_FERME = "http://localhost:1";
  private static final List<String> SERVICES_SIMULES =
      List.of(
          "ms-auth",
          "ms-besoins",
          "ms-achats",
          "ms-fournisseurs",
          "ms-suivi",
          "ms-reporting",
          "ms-chatbot",
          "ms-notification");

  private static final WireMockServer SERVICES = new WireMockServer(wireMockConfig().dynamicPort());

  static {
    SERVICES.start();
  }

  @DynamicPropertySource
  static void proprietes(DynamicPropertyRegistry registry) {
    SERVICES_SIMULES.forEach(
        s -> registry.add("cse.gateway.services." + s + ".url", SERVICES::baseUrl));
    registry.add("cse.gateway.services.ms-audit.url", () -> PORT_FERME);
  }

  @AfterAll
  static void arreter() {
    SERVICES.stop();
  }

  @Autowired private WebTestClient client;
  @Autowired private RouteLocator routes;

  @BeforeEach
  void initialiser() {
    SERVICES.resetAll();
    SERVICES.stubFor(get(urlEqualTo("/actuator/health")).willReturn(okJson("{\"status\":\"UP\"}")));
  }

  @Test
  void should_router_chaque_chemin_du_contrat_vers_son_service() {
    Map<String, String> attendus =
        Map.ofEntries(
            Map.entry("/api/me", "ms-auth"),
            Map.entry("/api/admin/utilisateurs/42/statut", "ms-auth"),
            Map.entry("/api/admin/roles", "ms-auth"),
            Map.entry("/api/besoins", "ms-besoins"),
            Map.entry("/api/besoins/1/valider", "ms-besoins"),
            Map.entry("/api/besoins-a-acheter", "ms-achats"),
            Map.entry("/api/appels-offres/1/offres", "ms-achats"),
            Map.entry("/api/contrats/1/signer", "ms-achats"),
            Map.entry("/api/engagements/1/valider", "ms-achats"),
            Map.entry("/api/factures", "ms-achats"),
            Map.entry("/api/finance/synthese", "ms-achats"),
            Map.entry("/api/fournisseurs/classement", "ms-fournisseurs"),
            Map.entry("/api/litiges/1/action", "ms-fournisseurs"),
            Map.entry("/api/materiels/carte", "ms-suivi"),
            Map.entry("/api/maintenances", "ms-suivi"),
            Map.entry("/api/chantiers/1", "ms-suivi"),
            Map.entry("/api/affectations/planning", "ms-suivi"),
            Map.entry("/api/pannes", "ms-suivi"),
            Map.entry("/api/referentiels/types-engins", "ms-suivi"),
            Map.entry("/api/reporting/kpis", "ms-reporting"),
            Map.entry("/api/chatbot/query", "ms-chatbot"),
            Map.entry("/api/notifications/non-lues/count", "ms-notification"),
            Map.entry("/api/notifications/stream", "ms-notification-sse"),
            Map.entry("/api/audit/statistiques", "ms-audit"),
            Map.entry("/api/portail/appels-offres", "ms-achats"),
            Map.entry("/api/portail/offres/mes-offres", "ms-achats"),
            Map.entry("/api/portail/attributions", "ms-achats"),
            Map.entry("/api/portail/factures", "ms-achats"),
            Map.entry("/api/portail/contrats", "ms-achats"),
            Map.entry("/api/portail/contrats/1/signer", "ms-achats"),
            Map.entry("/api/portail/profil", "ms-fournisseurs"),
            Map.entry("/api/portail/catalogue/1", "ms-fournisseurs"),
            Map.entry("/api/portail/documents", "ms-fournisseurs"),
            Map.entry("/api/portail/inscription", "ms-fournisseurs"),
            Map.entry("/api/portail/litiges", "ms-fournisseurs"),
            Map.entry("/v3/api-docs/ms-achats", "ms-achats-api-docs"));
    attendus.forEach(
        (chemin, route) -> assertThat(route(chemin).map(Route::getId)).as(chemin).contains(route));
    assertThat(route("/api/inconnu")).isEmpty();
    assertThat(route("/api/besoinsx")).isEmpty();

    Route sse = route("/api/notifications/stream").orElseThrow();
    assertThat(sse.getMetadata().get(RouteMetadataUtils.RESPONSE_TIMEOUT_ATTR))
        .isEqualTo(3_600_000L);
    assertThat(route("/api/audit").orElseThrow().getUri().toString()).isEqualTo(PORT_FERME);
  }

  @Test
  void should_repondre_401_problem_detail_et_correler_when_sans_jeton() {
    client
        .get()
        .uri("/api/besoins")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectHeader()
        .exists("X-Correlation-Id")
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("NON_AUTHENTIFIE")
        .jsonPath("$.status")
        .isEqualTo(401)
        .jsonPath("$.traceId")
        .isNotEmpty()
        .jsonPath("$.timestamp")
        .isNotEmpty();
    client
        .get()
        .uri("/api/besoins")
        .header(HttpHeaders.AUTHORIZATION, "Bearer jeton.invalide.signature")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void should_refuser_le_portail_when_role_fournisseur_absent() {
    client
        .get()
        .uri("/api/portail/offres/mes-offres")
        .header(HttpHeaders.AUTHORIZATION, JetonsDeTest.bearer(SUB, Roles.CHEF_CHANTIER))
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("ACCES_REFUSE");
    SERVICES.verify(0, getRequestedFor(urlEqualTo("/api/portail/offres/mes-offres")));
  }

  @Test
  void should_relayer_jeton_et_correlation_when_fournisseur_sur_le_portail() {
    SERVICES.stubFor(get(urlEqualTo("/api/portail/offres/mes-offres")).willReturn(okJson("[]")));
    String jeton = JetonsDeTest.bearer(SUB, Roles.FOURNISSEUR);
    client
        .get()
        .uri("/api/portail/offres/mes-offres")
        .header(HttpHeaders.AUTHORIZATION, jeton)
        .header("X-Correlation-Id", "test-correlation-42")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals("X-Correlation-Id", "test-correlation-42")
        .expectHeader()
        .valueEquals("X-Content-Type-Options", "nosniff")
        .expectHeader()
        .valueEquals("X-Frame-Options", "DENY");
    SERVICES.verify(
        getRequestedFor(urlEqualTo("/api/portail/offres/mes-offres"))
            .withHeader("X-Correlation-Id", equalTo("test-correlation-42"))
            .withHeader(HttpHeaders.AUTHORIZATION, equalTo(jeton)));
  }

  @Test
  void should_generer_une_correlation_when_en_tete_absent_ou_invalide() {
    SERVICES.stubFor(get(urlEqualTo("/api/besoins")).willReturn(okJson("{\"content\":[]}")));
    String genere =
        client
            .get()
            .uri("/api/besoins")
            .header(HttpHeaders.AUTHORIZATION, JetonsDeTest.bearer(SUB, Roles.RESPONSABLE_MATERIEL))
            .header("X-Correlation-Id", "invalide; injection\tlog")
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(String.class)
            .getResponseHeaders()
            .getFirst("X-Correlation-Id");
    assertThat(genere).matches("[0-9a-f-]{36}");
    SERVICES.verify(
        getRequestedFor(urlEqualTo("/api/besoins"))
            .withHeader("X-Correlation-Id", equalTo(genere)));
  }

  @Test
  void should_repondre_503_problem_detail_when_service_injoignable() {
    client
        .get()
        .uri("/api/audit")
        .header(HttpHeaders.AUTHORIZATION, JetonsDeTest.bearer(SUB, Roles.ADMIN_SYSTEME))
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("SERVICE_INDISPONIBLE")
        .jsonPath("$.detail")
        .value(d -> assertThat(d.toString()).contains("ms-audit").contains("indisponible"))
        .jsonPath("$.type")
        .isEqualTo("https://cse.sn/erreurs/service-indisponible")
        .jsonPath("$.traceId")
        .isNotEmpty();
  }

  @Test
  void should_repondre_404_problem_detail_when_route_inconnue() {
    client
        .get()
        .uri("/api/inconnu")
        .header(HttpHeaders.AUTHORIZATION, JetonsDeTest.bearer(SUB, Roles.DAF))
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("RESSOURCE_INTROUVABLE");
  }

  @Test
  void should_sonder_les_services_when_etat_de_la_plateforme() {
    client
        .get()
        .uri("/api/plateforme/etat")
        .header(HttpHeaders.AUTHORIZATION, JetonsDeTest.bearer(SUB, Roles.ADMIN_SYSTEME))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.services.length()")
        .isEqualTo(9)
        .jsonPath("$.services[0].nom")
        .isEqualTo("ms-auth")
        .jsonPath("$.services[0].statut")
        .isEqualTo("UP")
        .jsonPath("$.services[0].latenceMs")
        .isNumber()
        .jsonPath("$.services[8].nom")
        .isEqualTo("ms-audit")
        .jsonPath("$.services[8].statut")
        .isEqualTo("DOWN");
    client
        .get()
        .uri("/api/plateforme/etat")
        .header(HttpHeaders.AUTHORIZATION, JetonsDeTest.bearer(SUB, Roles.FOURNISSEUR))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void should_appliquer_cors_when_origine_autorisee_ou_non() {
    client
        .options()
        .uri("/api/besoins")
        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,x-correlation-id")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
        .expectHeader()
        .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    client
        .options()
        .uri("/api/besoins")
        .header(HttpHeaders.ORIGIN, "https://malveillant.example")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void should_agreger_la_documentation_des_services_when_swagger() {
    SERVICES.stubFor(
        get(urlEqualTo("/v3/api-docs"))
            .willReturn(
                okJson(
                    "{\"openapi\":\"3.1.0\",\"servers\":[{\"url\":\"http://ms-auth:8081\"}],"
                        + "\"paths\":{\"/api/me\":{}}}")));
    client
        .get()
        .uri("/v3/api-docs/ms-auth")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.servers[0].url")
        .isEqualTo("/")
        .jsonPath("$.paths['/api/me']")
        .exists();
    client
        .get()
        .uri("/v3/api-docs/ms-gateway")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.paths['/api/plateforme/etat']")
        .exists();
    client
        .get()
        .uri("/v3/api-docs/ms-gateway/swagger-config")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.urls[?(@.name == 'ms-auth')].url")
        .isEqualTo("/v3/api-docs/ms-auth")
        .jsonPath("$.urls.length()")
        .isEqualTo(10)
        .jsonPath("$.oauth2RedirectUrl")
        .value(u -> assertThat(u.toString()).contains("oauth2-redirect"));
    client.get().uri("/swagger-ui.html").exchange().expectStatus().is3xxRedirection();
    SERVICES.verify(
        getRequestedFor(urlEqualTo("/v3/api-docs")).withHeader("X-Correlation-Id", matching(".+")));
  }

  @Test
  void should_router_l_inscription_publique_sans_jeton_vers_ms_fournisseurs() {
    SERVICES.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/portail/inscription"))
            .willReturn(
                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                    .withStatus(201)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        "{\"id\":\"f0000000-0000-0000-0000-000000000099\","
                            + "\"raisonSociale\":\"Baobab Logistique\","
                            + "\"statut\":\"EN_ATTENTE_VALIDATION\"}")));
    client
        .post()
        .uri("/api/portail/inscription")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"raisonSociale\":\"Baobab Logistique\"}")
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.statut")
        .isEqualTo("EN_ATTENTE_VALIDATION");
    SERVICES.verify(
        com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
                urlEqualTo("/api/portail/inscription"))
            .withoutHeader(HttpHeaders.AUTHORIZATION));
    assertThat(route(HttpMethod.POST, "/api/portail/inscription").map(Route::getId))
        .contains("ms-fournisseurs-public-inscription");
  }

  @Test
  void should_garder_le_reste_du_portail_reserve_aux_fournisseurs_when_inscription_publique() {
    client
        .get()
        .uri("/api/portail/profil")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("NON_AUTHENTIFIE");
    // Seule la méthode POST est ouverte sur le chemin public.
    client.get().uri("/api/portail/inscription").exchange().expectStatus().isUnauthorized();
    client
        .put()
        .uri("/api/portail/inscription")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isUnauthorized();
    client
        .get()
        .uri("/api/portail/profil")
        .header(HttpHeaders.AUTHORIZATION, JetonsDeTest.bearer(SUB, Roles.RESPONSABLE_ACHATS))
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("ACCES_REFUSE");
    SERVICES.verify(0, getRequestedFor(urlEqualTo("/api/portail/profil")));
    SERVICES.verify(0, getRequestedFor(urlEqualTo("/api/portail/inscription")));
  }

  private Optional<Route> route(String chemin) {
    return route(HttpMethod.GET, chemin);
  }

  private Optional<Route> route(HttpMethod methode, String chemin) {
    MockServerWebExchange echange =
        MockServerWebExchange.from(MockServerHttpRequest.method(methode, chemin));
    return Optional.ofNullable(
        routes
            .getRoutes()
            .concatMap(
                r -> Mono.from(r.getPredicate().apply(echange)).filter(ok -> ok).map(ok -> r))
            .next()
            .block());
  }
}
