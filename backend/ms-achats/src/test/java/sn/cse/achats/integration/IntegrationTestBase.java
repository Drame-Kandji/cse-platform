package sn.cse.achats.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import sn.cse.achats.application.FauxPorts;
import sn.cse.common.test.PostgresTestConfiguration;

/**
 * Contexte Spring complet sur PostgreSQL (Testcontainers), partagé par les classes d'intégration :
 * ms-fournisseurs simulé par WireMock, horloge réglable, Kafka et relais d'outbox désactivés.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestConfiguration.class, IntegrationTestBase.HorlogeDeTest.class})
abstract class IntegrationTestBase {

  static final WireMockServer FOURNISSEURS = new WireMockServer(wireMockConfig().dynamicPort());

  static {
    FOURNISSEURS.start();
  }

  @DynamicPropertySource
  static void proprietes(DynamicPropertyRegistry registre) {
    registre.add("cse.achats.fournisseurs.base-url", FOURNISSEURS::baseUrl);
  }

  /** Horloge pilotée par les tests (remplace l'horloge système de cse-common). */
  @TestConfiguration(proxyBeanMethods = false)
  static class HorlogeDeTest {
    @Bean
    FauxPorts.Horloge horloge() {
      return new FauxPorts.Horloge(Instant.parse("2026-10-01T08:00:00Z"));
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired FauxPorts.Horloge horloge;

  static void fournisseurDistant(UUID id, String raisonSociale, String statut) {
    FOURNISSEURS.stubFor(
        get(urlEqualTo("/api/fournisseurs/" + id))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"id\":\""
                            + id
                            + "\",\"raisonSociale\":\""
                            + raisonSociale
                            + "\",\"statut\":\""
                            + statut
                            + "\",\"pays\":\"Sénégal\",\"specialites\":[\"MATERIEL_ROULANT\"]}")));
  }

  static void fournisseurIndisponible(UUID id) {
    FOURNISSEURS.stubFor(
        get(urlEqualTo("/api/fournisseurs/" + id)).willReturn(aResponse().withStatus(503)));
  }

  JsonNode json(ResultActions resultat) throws Exception {
    return mapper.readTree(resultat.andReturn().getResponse().getContentAsString());
  }
}
