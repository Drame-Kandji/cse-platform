package sn.cse.besoins.infrastructure.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClient;
import sn.cse.besoins.application.exception.ServiceSuiviIndisponibleException;
import sn.cse.besoins.application.port.out.AffectationMaterielPort.DemandeAffectation;
import sn.cse.besoins.application.port.out.AffectationMaterielPort.ResultatAffectation;
import sn.cse.besoins.application.port.out.ChantierPort.ChantierInfo;
import sn.cse.besoins.application.port.out.DisponibiliteMaterielPort.DemandeDisponibilite;
import sn.cse.besoins.application.port.out.DisponibiliteMaterielPort.Disponibilite;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.infrastructure.config.BesoinsProperties;
import sn.cse.common.client.ServiceClientProperties;
import sn.cse.common.client.ServiceRestClientFactory;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.security.CurrentUserProvider;

/**
 * Adaptateur ms-suivi contre un ms-suivi simulé (WireMock) avec la vraie configuration Resilience4j
 * (aspects AOP) : rejeu, disjoncteur et repli.
 */
@SpringJUnitConfig(SuiviRestAdapterTest.Contexte.class)
@TestPropertySource(
    properties = {
      "resilience4j.retry.instances.suivi.max-attempts=3",
      "resilience4j.retry.instances.suivi.wait-duration=10ms",
      "resilience4j.retry.instances.suivi.enable-exponential-backoff=true",
      "resilience4j.retry.instances.suivi.exponential-backoff-multiplier=2",
      "resilience4j.retry.instances.suivi.retry-exceptions[0]=org.springframework.web.client.ResourceAccessException",
      "resilience4j.retry.instances.suivi.retry-exceptions[1]=sn.cse.besoins.infrastructure.client.SuiviErreurTechniqueException",
      "resilience4j.retry.instances.suivi.ignore-exceptions[0]=sn.cse.common.exception.CseException",
      "resilience4j.circuitbreaker.instances.suivi.sliding-window-size=4",
      "resilience4j.circuitbreaker.instances.suivi.minimum-number-of-calls=4",
      "resilience4j.circuitbreaker.instances.suivi.failure-rate-threshold=50",
      "resilience4j.circuitbreaker.instances.suivi.wait-duration-in-open-state=60s",
      "resilience4j.circuitbreaker.instances.suivi.ignore-exceptions[0]=sn.cse.common.exception.CseException"
    })
class SuiviRestAdapterTest {

  private static final WireMockServer SUIVI = new WireMockServer(options().dynamicPort());

  static {
    SUIVI.start();
  }

  private static final UUID CHANTIER = UUID.fromString("c0000000-0000-0000-0000-000000000001");
  private static final UUID MATERIEL = UUID.fromString("a1000000-0000-0000-0000-000000000001");
  private static final LocalDate DU = LocalDate.parse("2026-11-15");

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration({
    AopAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class,
    RestClientAutoConfiguration.class,
    RetryAutoConfiguration.class,
    CircuitBreakerAutoConfiguration.class
  })
  static class Contexte {

    @Bean
    SuiviRestAdapter suiviRestAdapter(RestClient.Builder builder) {
      ServiceRestClientFactory factory =
          new ServiceRestClientFactory(
              builder,
              new CurrentUserProvider(),
              new ServiceClientProperties(Duration.ofMillis(500), Duration.ofMillis(800)));
      BesoinsProperties properties =
          new BesoinsProperties(
              new BesoinsProperties.Reference("BES", 4),
              new BesoinsProperties.Consolidation(30, 100),
              new BesoinsProperties.Validation(
                  List.of(
                      new BesoinsProperties.Etape("RM", "RESPONSABLE_MATERIEL", true, null, null))),
              new BesoinsProperties.Suivi(SUIVI.baseUrl()));
      return new SuiviRestAdapter(factory, properties);
    }
  }

  @Autowired SuiviRestAdapter adapter;
  @Autowired CircuitBreakerRegistry disjoncteurs;

  @AfterAll
  static void arreter() {
    SUIVI.stop();
  }

  @BeforeEach
  void reinitialiser() {
    SUIVI.resetAll();
    disjoncteurs.circuitBreaker(SuiviRestAdapter.INSTANCE).reset();
  }

  @Test
  void should_lire_un_chantier_actif_ou_termine_et_ignorer_un_chantier_inconnu() {
    SUIVI.stubFor(
        get("/api/chantiers/" + CHANTIER)
            .willReturn(
                okJson(
                    "{\"id\":\""
                        + CHANTIER
                        + "\",\"code\":\"CH-DKR-01\","
                        + "\"nom\":\"Extension autoroute Dakar-AIBD\",\"statut\":\"EN_COURS\","
                        + "\"budget\":1000}")));
    UUID termine = UUID.randomUUID();
    SUIVI.stubFor(
        get("/api/chantiers/" + termine)
            .willReturn(
                okJson("{\"id\":\"" + termine + "\",\"nom\":\"Station\",\"statut\":\"TERMINE\"}")));
    UUID inconnu = UUID.randomUUID();
    SUIVI.stubFor(get("/api/chantiers/" + inconnu).willReturn(aResponse().withStatus(404)));

    assertThat(adapter.trouver(CHANTIER))
        .contains(new ChantierInfo(CHANTIER, "Extension autoroute Dakar-AIBD", true));
    assertThat(adapter.trouver(termine)).get().extracting(ChantierInfo::actif).isEqualTo(false);
    assertThat(adapter.trouver(inconnu)).isEmpty();
  }

  @Test
  void should_interroger_la_disponibilite_avec_les_criteres_du_contrat() {
    SUIVI.stubFor(
        get(urlPathEqualTo("/api/materiels/disponibilite"))
            .willReturn(
                okJson(
                    "{\"disponible\":true,\"quantiteDemandee\":2,\"quantiteDisponible\":2,"
                        + "\"materiels\":[{\"id\":\""
                        + MATERIEL
                        + "\",\"code\":\"PEL-01\"},"
                        + "{\"id\":\"a1000000-0000-0000-0000-000000000002\",\"code\":\"PEL-02\"}]}")));

    Disponibilite d =
        adapter.verifier(
            new DemandeDisponibilite(
                CategorieMateriel.MATERIEL_ROULANT,
                "Pelle hydraulique 20 t",
                2,
                DU,
                DU.plusDays(30)));

    assertThat(d.quantiteDisponible()).isEqualTo(2);
    assertThat(d.materielIds()).hasSize(2).first().isEqualTo(MATERIEL);
    SUIVI.verify(
        getRequestedFor(urlPathEqualTo("/api/materiels/disponibilite"))
            .withQueryParam("categorie", equalTo("MATERIEL_ROULANT"))
            .withQueryParam("designation", equalTo("Pelle hydraulique 20 t"))
            .withQueryParam("quantite", equalTo("2"))
            .withQueryParam("du", equalTo("2026-11-15"))
            .withQueryParam("au", equalTo("2026-12-15")));
  }

  @Test
  void should_affecter_un_materiel_ou_signaler_un_refus_metier() {
    UUID besoin = UUID.randomUUID();
    UUID affectation = UUID.randomUUID();
    SUIVI.stubFor(
        post("/api/materiels/" + MATERIEL + "/affecter")
            .willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"id\":\"" + affectation + "\",\"materielId\":\"" + MATERIEL + "\"}")));
    UUID occupe = UUID.randomUUID();
    SUIVI.stubFor(
        post("/api/materiels/" + occupe + "/affecter")
            .willReturn(
                aResponse()
                    .withStatus(409)
                    .withHeader("Content-Type", "application/problem+json")
                    .withBody("{\"status\":409,\"code\":\"CONFLIT_AFFECTATION\"}")));

    ResultatAffectation ok =
        adapter.affecter(new DemandeAffectation(MATERIEL, CHANTIER, besoin, DU, DU.plusDays(30)));
    ResultatAffectation refus =
        adapter.affecter(new DemandeAffectation(occupe, CHANTIER, besoin, DU, DU.plusDays(30)));

    assertThat(ok).isEqualTo(new ResultatAffectation.Affecte(affectation));
    assertThat(refus).isEqualTo(new ResultatAffectation.Refuse("CONFLIT_AFFECTATION"));
    SUIVI.verify(
        postRequestedFor(urlPathEqualTo("/api/materiels/" + MATERIEL + "/affecter"))
            .withRequestBody(
                equalToJson(
                    "{\"chantierId\":\""
                        + CHANTIER
                        + "\",\"besoinId\":\""
                        + besoin
                        + "\","
                        + "\"dateDebut\":\"2026-11-15\",\"dateFinPrevue\":\"2026-12-15\"}")));
  }

  @Test
  void should_rejouer_une_erreur_transitoire_puis_reussir() {
    SUIVI.stubFor(
        get("/api/chantiers/" + CHANTIER)
            .inScenario("transitoire")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("retabli"));
    SUIVI.stubFor(
        get("/api/chantiers/" + CHANTIER)
            .inScenario("transitoire")
            .whenScenarioStateIs("retabli")
            .willReturn(
                okJson("{\"id\":\"" + CHANTIER + "\",\"nom\":\"A\",\"statut\":\"EN_COURS\"}")));

    assertThat(adapter.trouver(CHANTIER)).isPresent();
    SUIVI.verify(2, getRequestedFor(urlPathEqualTo("/api/chantiers/" + CHANTIER)));
  }

  @Test
  void should_basculer_sur_le_repli_when_ms_suivi_est_en_panne() {
    SUIVI.stubFor(
        get(urlPathEqualTo("/api/materiels/disponibilite"))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    assertThatThrownBy(
            () ->
                adapter.verifier(
                    new DemandeDisponibilite(CategorieMateriel.OUTILLAGE, "Bétonnière", 1, DU, DU)))
        .isInstanceOf(ServiceSuiviIndisponibleException.class)
        .hasFieldOrPropertyWithValue("code", "SERVICE_SUIVI_INDISPONIBLE");
    SUIVI.verify(3, getRequestedFor(urlPathEqualTo("/api/materiels/disponibilite")));
  }

  @Test
  void should_basculer_sur_le_repli_when_ms_suivi_ne_repond_pas_dans_le_delai() {
    SUIVI.stubFor(
        post(urlPathEqualTo("/api/materiels/" + MATERIEL + "/affecter"))
            .willReturn(aResponse().withStatus(201).withFixedDelay(2_000)));

    assertThatThrownBy(
            () ->
                adapter.affecter(
                    new DemandeAffectation(MATERIEL, CHANTIER, UUID.randomUUID(), DU, DU)))
        .isInstanceOf(ServiceSuiviIndisponibleException.class);
  }

  @Test
  void should_ouvrir_le_disjoncteur_apres_des_pannes_repetees() {
    SUIVI.stubFor(
        get(urlPathEqualTo("/api/chantiers/" + CHANTIER)).willReturn(aResponse().withStatus(500)));

    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(() -> adapter.trouver(CHANTIER))
          .isInstanceOf(ServiceSuiviIndisponibleException.class);
    }
    assertThat(disjoncteurs.circuitBreaker(SuiviRestAdapter.INSTANCE).getState())
        .isEqualTo(CircuitBreaker.State.OPEN);
    SUIVI.resetRequests();
    assertThatThrownBy(() -> adapter.trouver(CHANTIER))
        .isInstanceOf(ServiceSuiviIndisponibleException.class);
    SUIVI.verify(0, getRequestedFor(urlPathEqualTo("/api/chantiers/" + CHANTIER)));
  }

  @Test
  void should_propager_un_refus_d_acces_sans_rejouer() {
    SUIVI.stubFor(get("/api/chantiers/" + CHANTIER).willReturn(aResponse().withStatus(403)));

    assertThatThrownBy(() -> adapter.trouver(CHANTIER))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasFieldOrPropertyWithValue("code", "ACCES_SUIVI_REFUSE");
    SUIVI.verify(1, getRequestedFor(urlPathEqualTo("/api/chantiers/" + CHANTIER)));
  }
}
