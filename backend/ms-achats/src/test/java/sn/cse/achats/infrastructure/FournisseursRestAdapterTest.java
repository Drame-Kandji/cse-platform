package sn.cse.achats.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import sn.cse.achats.application.port.out.FournisseurProjectionRepository;
import sn.cse.achats.domain.model.FournisseurConnu;
import sn.cse.achats.domain.model.StatutFournisseur;
import sn.cse.achats.infrastructure.client.FournisseursRestAdapter;
import sn.cse.common.exception.ServiceUnavailableException;

/** Adaptateur REST vers ms-fournisseurs : réponses réelles simulées par WireMock et repli local. */
class FournisseursRestAdapterTest {

  private static final UUID ID = UUID.fromString("f0000000-0000-0000-0000-000000000001");
  private static final Instant MAINTENANT = Instant.parse("2026-10-30T09:00:00Z");
  private static final String CHEMIN = "/api/fournisseurs/" + ID;

  private static WireMockServer wiremock;

  private FournisseurProjectionRepository projection;
  private CircuitBreaker circuitBreaker;
  private FournisseursRestAdapter adaptateur;

  @BeforeAll
  static void demarrer() {
    wiremock = new WireMockServer(wireMockConfig().dynamicPort());
    wiremock.start();
  }

  @AfterAll
  static void arreter() {
    wiremock.stop();
  }

  @BeforeEach
  void preparer() {
    wiremock.resetAll();
    projection = mock(FournisseurProjectionRepository.class);
    circuitBreaker =
        CircuitBreaker.of(
            "fournisseurs",
            CircuitBreakerConfig.custom().slidingWindowSize(2).minimumNumberOfCalls(2).build());
    Retry retry =
        Retry.of(
            "fournisseurs",
            RetryConfig.custom().maxAttempts(2).waitDuration(Duration.ofMillis(10)).build());
    SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
    http.setConnectTimeout(Duration.ofSeconds(1));
    http.setReadTimeout(Duration.ofSeconds(1));
    RestClient client =
        RestClient.builder().baseUrl(wiremock.baseUrl()).requestFactory(http).build();
    adaptateur =
        new FournisseursRestAdapter(
            client, circuitBreaker, retry, projection, Clock.fixed(MAINTENANT, ZoneOffset.UTC));
  }

  private static void repondre(int statut, String corps) {
    wiremock.stubFor(
        get(urlEqualTo(CHEMIN))
            .willReturn(
                aResponse()
                    .withStatus(statut)
                    .withHeader("Content-Type", "application/json")
                    .withBody(corps)));
  }

  @Test
  void should_lire_le_fournisseur_et_rafraichir_la_projection() {
    repondre(
        200,
        "{\"id\":\""
            + ID
            + "\",\"raisonSociale\":\"Sahel Engins SA\",\"statut\":\"ACTIF\",\"ninea\":\"0048\",\"documents\":[]}");

    Optional<FournisseurConnu> resultat = adaptateur.verifier(ID);

    assertThat(resultat)
        .contains(new FournisseurConnu(ID, "Sahel Engins SA", StatutFournisseur.ACTIF));
    verify(projection).enregistrerSiPlusRecent(resultat.get(), MAINTENANT);
  }

  @Test
  void should_renvoyer_vide_sans_repli_when_fournisseur_inexistant() {
    repondre(404, "{\"code\":\"FOURNISSEUR_INTROUVABLE\"}");

    assertThat(adaptateur.verifier(ID)).isEmpty();
    verify(projection, never()).parId(any());
    wiremock.verify(1, getRequestedFor(urlEqualTo(CHEMIN)));
  }

  @Test
  void should_rejouer_puis_se_replier_sur_la_projection_when_erreur_serveur() {
    repondre(503, "{}");
    FournisseurConnu local =
        new FournisseurConnu(ID, "Sahel Engins SA", StatutFournisseur.SUSPENDU);
    when(projection.parId(ID)).thenReturn(Optional.of(local));

    assertThat(adaptateur.verifier(ID)).contains(local);
    wiremock.verify(2, getRequestedFor(urlEqualTo(CHEMIN)));
    verify(projection, never()).enregistrerSiPlusRecent(any(), eq(MAINTENANT));
  }

  @Test
  void should_se_replier_when_reponse_incomplete_ou_service_injoignable() {
    repondre(200, "{\"id\":\"" + ID + "\"}");
    when(projection.parId(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adaptateur.verifier(ID))
        .isInstanceOf(ServiceUnavailableException.class)
        .hasMessageContaining("indisponible");
    verify(projection).parId(ID);
  }

  @Test
  void should_court_circuiter_les_appels_when_circuit_ouvert() {
    circuitBreaker.transitionToOpenState();
    FournisseurConnu local = new FournisseurConnu(ID, "Sahel Engins SA", StatutFournisseur.ACTIF);
    when(projection.parId(ID)).thenReturn(Optional.of(local));

    assertThat(adaptateur.verifier(ID)).contains(local);
    wiremock.verify(0, getRequestedFor(urlEqualTo(CHEMIN)));
  }
}
