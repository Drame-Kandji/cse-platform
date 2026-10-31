package sn.cse.chatbot.infrastructure.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import sn.cse.chatbot.application.exception.SourceIndisponibleException;
import sn.cse.chatbot.application.port.out.CritereMateriel;
import sn.cse.chatbot.domain.model.MaterielInfo;
import sn.cse.chatbot.infrastructure.Proprietes;
import sn.cse.common.client.ServiceClientProperties;
import sn.cse.common.client.ServiceRestClientFactory;
import sn.cse.common.security.CurrentUserProvider;

/** Client ms-suivi contre un serveur HTTP simulé (WireMock). */
class SuiviRestClientTest {

  private static final WireMockServer WM = new WireMockServer(wireMockConfig().dynamicPort());
  private SuiviRestClient client;

  @BeforeAll
  static void demarrer() {
    WM.start();
  }

  @AfterAll
  static void arreter() {
    WM.stop();
  }

  @BeforeEach
  void preparer() {
    WM.resetAll();
    Jwt jwt =
        new Jwt(
            "jeton-utilisateur",
            Instant.EPOCH,
            Instant.MAX,
            Map.of("alg", "none"),
            Map.of("sub", "u1"));
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    client =
        new SuiviRestClient(
            new ServiceRestClientFactory(
                RestClient.builder(),
                new CurrentUserProvider(),
                new ServiceClientProperties(null, null)),
            Proprietes.chatbot(WM.baseUrl()));
  }

  @AfterEach
  void nettoyer() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_map_materiels_and_relay_user_jwt() {
    WM.stubFor(
        get(urlPathEqualTo("/api/materiels"))
            .willReturn(
                okJson(
                    """
        {"content":[{"id":"m1","code":"NIV-002","designation":"Niveleuse CAT 140K","categorie":"MATERIEL_ROULANT",
          "etat":"AFFECTE","chantierActuelId":"c2","chantierActuelNom":"Contournement de Thiès RN2","latitude":14.79,
          "longitude":-16.92,"prochaineMaintenance":"2026-11-20","maintenanceDue":false,"champInconnu":"ignoré"}],
         "page":0,"size":5,"totalElements":1,"totalPages":1,"first":true,"last":true}""")));

    List<MaterielInfo> r =
        client.rechercherMateriels(
            new CritereMateriel("niveleuse", "MATERIEL_ROULANT", null, null), 5);

    assertThat(r)
        .singleElement()
        .satisfies(
            m -> {
              assertThat(m.code()).isEqualTo("NIV-002");
              assertThat(m.chantierNom()).isEqualTo("Contournement de Thiès RN2");
              assertThat(m.prochaineMaintenance()).isEqualTo(LocalDate.of(2026, 11, 20));
            });
    WM.verify(
        getRequestedFor(urlPathEqualTo("/api/materiels"))
            .withHeader("Authorization", equalTo("Bearer jeton-utilisateur"))
            .withQueryParam("q", equalTo("niveleuse"))
            .withQueryParam("categorie", equalTo("MATERIEL_ROULANT"))
            .withQueryParam("size", equalTo("5")));
  }

  @Test
  void should_return_empty_list_when_access_denied_or_not_found() {
    WM.stubFor(get(urlPathEqualTo("/api/maintenances")).willReturn(aResponse().withStatus(403)));
    WM.stubFor(get(urlPathEqualTo("/api/chantiers")).willReturn(aResponse().withStatus(404)));
    assertThat(client.maintenances("m1", 5)).isEmpty();
    assertThat(client.rechercherChantiers("Thiès", 3)).isEmpty();
  }

  @Test
  void should_read_planning_array_and_paged_affectations() {
    String affectation =
        """
        {"id":"a1","materielId":"m1","materielCode":"NIV-002","materielDesignation":"Niveleuse","categorie":"MATERIEL_ROULANT",
         "chantierId":"c2","chantierNom":"Thiès","dateDebut":"2026-10-01","dateFinPrevue":"2026-12-31","statut":"EN_COURS"}""";
    WM.stubFor(
        get(urlPathEqualTo("/api/affectations/planning"))
            .willReturn(okJson("[" + affectation + "," + affectation + "]")));
    WM.stubFor(
        get(urlPathEqualTo("/api/affectations"))
            .willReturn(okJson("{\"content\":[" + affectation + "]}")));
    WM.stubFor(
        get(urlPathEqualTo("/api/chantiers"))
            .willReturn(
                okJson(
                    "{\"content\":[{\"id\":\"c2\",\"code\":\"CH-THS-01\",\"nom\":\"Contournement de Thiès RN2\",\"statut\":\"EN_COURS\"}]}")));
    WM.stubFor(
        get(urlPathEqualTo("/api/maintenances"))
            .willReturn(
                okJson(
                    "{\"content\":[{\"id\":\"o1\",\"numeroOt\":\"OT-2026-0007\",\"materielId\":\"m1\",\"type\":\"PREVENTIVE\",\"statut\":\"CLOTUREE\",\"datePlanifiee\":\"2026-09-01\",\"cout\":450000}]}")));

    assertThat(client.planning("c2", LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 29), 1))
        .hasSize(1);
    WM.verify(
        getRequestedFor(urlPathEqualTo("/api/affectations/planning"))
            .withQueryParam("du", equalTo("2026-10-30"))
            .withQueryParam("au", equalTo("2026-11-29"))
            .withQueryParam("chantierId", equalTo("c2")));
    assertThat(client.affectationsDuMateriel("m1", 5))
        .singleElement()
        .satisfies(a -> assertThat(a.chantierNom()).isEqualTo("Thiès"));
    assertThat(client.rechercherChantiers("Thiès", 3))
        .singleElement()
        .satisfies(c -> assertThat(c.code()).isEqualTo("CH-THS-01"));
    assertThat(client.maintenances(null, 5))
        .singleElement()
        .satisfies(m -> assertThat(m.cout()).isEqualByComparingTo("450000"));
  }

  @Test
  void should_surface_server_errors_and_convert_them_in_fallbacks() {
    WM.stubFor(get(urlPathEqualTo("/api/materiels")).willReturn(aResponse().withStatus(503)));
    CritereMateriel c = new CritereMateriel(null, null, "DISPONIBLE", null);
    assertThatThrownBy(() -> client.rechercherMateriels(c, 5))
        .isInstanceOf(HttpServerErrorException.class);
    IllegalStateException cause = new IllegalStateException("délai");
    assertThatThrownBy(() -> client.materielsIndisponibles(c, 5, cause))
        .isInstanceOf(SourceIndisponibleException.class);
    assertThatThrownBy(() -> client.maintenancesIndisponibles("m", 5, cause))
        .isInstanceOf(SourceIndisponibleException.class);
    assertThatThrownBy(() -> client.planningIndisponible(null, null, null, 5, cause))
        .isInstanceOf(SourceIndisponibleException.class);
    assertThatThrownBy(() -> client.affectationsIndisponibles("m", 5, cause))
        .isInstanceOf(SourceIndisponibleException.class);
    SourceIndisponibleException deja = new SourceIndisponibleException("suivi", cause);
    assertThatThrownBy(() -> client.chantiersIndisponibles("q", 5, deja)).isSameAs(deja);
  }
}
