package sn.cse.auth.infrastructure.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import sn.cse.auth.application.exception.CompteIdentiteIntrouvableException;
import sn.cse.auth.application.exception.IdentiteIndisponibleException;
import sn.cse.auth.application.port.out.CompteIdentite;
import sn.cse.auth.application.port.out.NouveauCompte;
import sn.cse.auth.application.port.out.ProfilIdentite;
import sn.cse.auth.domain.exception.EmailDejaUtiliseException;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur.Identite;
import sn.cse.common.exception.BusinessRuleException;

class KeycloakAdminAdapterTest {

  private static final String TOKEN = "/realms/cse/protocol/openid-connect/token";
  private static final String ADMIN = "/admin/realms/cse";
  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000901");
  private static final UUID CHANTIER = UUID.fromString("c0000000-0000-0000-0000-000000000001");
  private static final Identite IDENTITE = new Identite("awa.diallo@cse.sn", "Awa", "Diallo", null);

  private static WireMockServer keycloak;
  private KeycloakAdminAdapter adapter;

  @BeforeAll
  static void demarrer() {
    keycloak = new WireMockServer(wireMockConfig().dynamicPort());
    keycloak.start();
  }

  @AfterAll
  static void arreter() {
    keycloak.stop();
  }

  @BeforeEach
  void initialiser() {
    keycloak.resetAll();
    keycloak.stubFor(
        post(urlEqualTo(TOKEN))
            .willReturn(okJson("{\"access_token\":\"jeton-1\",\"expires_in\":300}")));
    adapter =
        new KeycloakAdminAdapter(
            RestClient.builder(), proprietes(keycloak.baseUrl()), Clock.systemUTC());
  }

  private static KeycloakProperties proprietes(String url) {
    return new KeycloakProperties(
        url,
        "cse",
        "cse-admin-api",
        "secret-test",
        Duration.ofSeconds(1),
        Duration.ofSeconds(2),
        Duration.ofSeconds(30),
        2);
  }

  @Test
  void should_creer_le_compte_avec_mot_de_passe_temporaire_et_otp_when_role_sensible() {
    keycloak.stubFor(
        post(urlEqualTo(ADMIN + "/users"))
            .willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader("Location", keycloak.baseUrl() + ADMIN + "/users/" + ID)));

    UUID id =
        adapter.creerCompte(
            new NouveauCompte(
                IDENTITE,
                Role.DAF,
                new AttributsProfil(null, null, null, new BigDecimal("5000000000"), null),
                "Temp@2026!",
                true));

    assertThat(id).isEqualTo(ID);
    keycloak.verify(
        postRequestedFor(urlEqualTo(TOKEN))
            .withRequestBody(
                equalTo(
                    "grant_type=client_credentials&client_id=cse-admin-api&client_secret=secret-test")));
    keycloak.verify(
        postRequestedFor(urlEqualTo(ADMIN + "/users"))
            .withHeader("Authorization", equalTo("Bearer jeton-1"))
            .withRequestBody(matchingJsonPath("$.username", equalTo("awa.diallo@cse.sn")))
            .withRequestBody(matchingJsonPath("$.enabled", equalTo("true")))
            .withRequestBody(matchingJsonPath("$.credentials[0].temporary", equalTo("true")))
            .withRequestBody(matchingJsonPath("$.credentials[0].value", equalTo("Temp@2026!")))
            .withRequestBody(matchingJsonPath("$.attributes.budget_gere[0]", equalTo("5000000000")))
            .withRequestBody(
                matchingJsonPath(
                    "$.requiredActions", equalToJson("[\"UPDATE_PASSWORD\",\"CONFIGURE_TOTP\"]"))));
  }

  @Test
  void should_exiger_seulement_le_changement_de_mot_de_passe_when_role_non_sensible() {
    keycloak.stubFor(
        post(urlEqualTo(ADMIN + "/users"))
            .willReturn(
                aResponse().withStatus(201).withHeader("Location", ADMIN + "/users/" + ID)));
    adapter.creerCompte(
        new NouveauCompte(
            IDENTITE,
            Role.CHEF_CHANTIER,
            new AttributsProfil(Set.of(CHANTIER), null, null, null, null),
            "Temp@2026!",
            false));
    keycloak.verify(
        postRequestedFor(urlEqualTo(ADMIN + "/users"))
            .withRequestBody(
                matchingJsonPath("$.requiredActions", equalToJson("[\"UPDATE_PASSWORD\"]")))
            .withRequestBody(
                matchingJsonPath("$.attributes.chantier_ids[0]", equalTo(CHANTIER.toString()))));
  }

  @Test
  void should_reutiliser_le_jeton_en_cache_when_appels_successifs() {
    keycloak.stubFor(
        get(urlEqualTo(ADMIN + "/users/" + ID + "/credentials")).willReturn(okJson("[]")));
    adapter.otpConfigure(ID);
    adapter.otpConfigure(ID);
    keycloak.verify(1, postRequestedFor(urlEqualTo(TOKEN)));
  }

  @Test
  void should_renouveler_le_jeton_when_keycloak_repond_401() {
    keycloak.stubFor(
        get(urlEqualTo(ADMIN + "/users/" + ID + "/credentials"))
            .withHeader("Authorization", equalTo("Bearer jeton-1"))
            .willReturn(aResponse().withStatus(401)));
    keycloak.stubFor(
        get(urlEqualTo(ADMIN + "/users/" + ID + "/credentials"))
            .withHeader("Authorization", equalTo("Bearer jeton-2"))
            .willReturn(okJson("[{\"type\":\"password\"},{\"type\":\"otp\"}]")));
    keycloak.stubFor(
        post(urlEqualTo(TOKEN))
            .inScenario("renouvellement")
            .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
            .willReturn(okJson("{\"access_token\":\"jeton-1\",\"expires_in\":300}"))
            .willSetStateTo("second"));
    keycloak.stubFor(
        post(urlEqualTo(TOKEN))
            .inScenario("renouvellement")
            .whenScenarioStateIs("second")
            .willReturn(okJson("{\"access_token\":\"jeton-2\",\"expires_in\":300}")));

    assertThat(adapter.otpConfigure(ID)).isTrue();
    keycloak.verify(2, postRequestedFor(urlEqualTo(TOKEN)));
  }

  @Test
  void should_attribuer_un_seul_role_cse_when_assignation() {
    String mappings = ADMIN + "/users/" + ID + "/role-mappings/realm";
    keycloak.stubFor(
        get(urlEqualTo(mappings))
            .willReturn(
                okJson(
                    "[{\"id\":\"r0\",\"name\":\"default-roles-cse\"},"
                        + "{\"id\":\"r1\",\"name\":\"CHEF_CHANTIER\"}]")));
    keycloak.stubFor(delete(urlEqualTo(mappings)).willReturn(aResponse().withStatus(204)));
    keycloak.stubFor(
        get(urlEqualTo(ADMIN + "/roles/DAF"))
            .willReturn(okJson("{\"id\":\"r4\",\"name\":\"DAF\"}")));
    keycloak.stubFor(post(urlEqualTo(mappings)).willReturn(aResponse().withStatus(204)));

    adapter.assignerRole(ID, Role.DAF);

    keycloak.verify(
        deleteRequestedFor(urlEqualTo(mappings))
            .withRequestBody(equalToJson("[{\"id\":\"r1\",\"name\":\"CHEF_CHANTIER\"}]")));
    keycloak.verify(
        postRequestedFor(urlEqualTo(mappings))
            .withRequestBody(equalToJson("[{\"id\":\"r4\",\"name\":\"DAF\"}]")));
  }

  @Test
  void should_ne_rien_changer_when_role_deja_attribue() {
    String mappings = ADMIN + "/users/" + ID + "/role-mappings/realm";
    keycloak.stubFor(
        get(urlEqualTo(mappings)).willReturn(okJson("[{\"id\":\"r4\",\"name\":\"DAF\"}]")));
    adapter.assignerRole(ID, Role.DAF);
    keycloak.verify(0, deleteRequestedFor(urlEqualTo(mappings)));
    keycloak.verify(0, postRequestedFor(urlEqualTo(mappings)));
  }

  @Test
  void should_preserver_les_attributs_non_geres_when_mise_a_jour() {
    keycloak.stubFor(
        get(urlEqualTo(ADMIN + "/users/" + ID))
            .willReturn(
                okJson(
                    "{\"id\":\""
                        + ID
                        + "\",\"username\":\"awa.diallo@cse.sn\",\"enabled\":true,"
                        + "\"attributes\":{\"locale\":[\"fr\"],\"chantier_ids\":[\"ancien\"]},"
                        + "\"requiredActions\":[]}")));
    keycloak.stubFor(
        put(urlEqualTo(ADMIN + "/users/" + ID)).willReturn(aResponse().withStatus(204)));

    adapter.mettreAJourProfil(
        ID,
        new ProfilIdentite(
            new Identite("awa.d@cse.sn", "Awa", "Diallo", null),
            new AttributsProfil(Set.of(CHANTIER), null, null, null, null)));
    adapter.definirActif(ID, false);
    adapter.exigerConfigurationOtp(ID);

    keycloak.verify(
        putRequestedFor(urlEqualTo(ADMIN + "/users/" + ID))
            .withRequestBody(matchingJsonPath("$.email", equalTo("awa.d@cse.sn")))
            .withRequestBody(matchingJsonPath("$.attributes.locale[0]", equalTo("fr")))
            .withRequestBody(
                matchingJsonPath("$.attributes.chantier_ids[0]", equalTo(CHANTIER.toString()))));
    keycloak.verify(
        putRequestedFor(urlEqualTo(ADMIN + "/users/" + ID))
            .withRequestBody(matchingJsonPath("$.enabled", equalTo("false"))));
    keycloak.verify(
        putRequestedFor(urlEqualTo(ADMIN + "/users/" + ID))
            .withRequestBody(matchingJsonPath("$.requiredActions[0]", equalTo("CONFIGURE_TOTP"))));
  }

  @Test
  void should_definir_un_mot_de_passe_temporaire_when_reinitialisation() {
    keycloak.stubFor(
        put(urlEqualTo(ADMIN + "/users/" + ID + "/reset-password"))
            .willReturn(aResponse().withStatus(204)));
    adapter.reinitialiserMotDePasse(ID, "Nouveau@2026");
    keycloak.verify(
        putRequestedFor(urlEqualTo(ADMIN + "/users/" + ID + "/reset-password"))
            .withRequestBody(
                equalToJson(
                    "{\"type\":\"password\",\"value\":\"Nouveau@2026\",\"temporary\":true}")));
  }

  @Test
  void should_traduire_les_erreurs_http_when_keycloak_refuse() {
    keycloak.stubFor(post(urlEqualTo(ADMIN + "/users")).willReturn(aResponse().withStatus(409)));
    NouveauCompte compte = new NouveauCompte(IDENTITE, Role.DAF, AttributsProfil.AUCUN, "x", false);
    assertThatThrownBy(() -> adapter.creerCompte(compte))
        .isInstanceOf(EmailDejaUtiliseException.class);

    keycloak.stubFor(
        put(urlEqualTo(ADMIN + "/users/" + ID + "/reset-password"))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"invalidPasswordMinLengthMessage\"}")));
    assertThatThrownBy(() -> adapter.reinitialiserMotDePasse(ID, "court"))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", "MOT_DE_PASSE_NON_CONFORME");

    keycloak.stubFor(
        get(urlEqualTo(ADMIN + "/users/" + ID)).willReturn(aResponse().withStatus(404)));
    assertThatThrownBy(() -> adapter.definirActif(ID, true))
        .isInstanceOf(CompteIdentiteIntrouvableException.class);

    keycloak.stubFor(
        get(urlEqualTo(ADMIN + "/users/" + ID + "/credentials"))
            .willReturn(aResponse().withStatus(503)));
    assertThatThrownBy(() -> adapter.otpConfigure(ID))
        .isInstanceOf(IdentiteIndisponibleException.class);
  }

  @Test
  void should_signaler_indisponibilite_when_keycloak_injoignable() {
    KeycloakAdminAdapter horsLigne =
        new KeycloakAdminAdapter(
            RestClient.builder(), proprietes("http://localhost:1"), Clock.systemUTC());
    assertThatThrownBy(horsLigne::listerComptes).isInstanceOf(IdentiteIndisponibleException.class);
  }

  @Test
  void should_ignorer_un_compte_deja_supprime_when_suppression() {
    keycloak.stubFor(
        delete(urlEqualTo(ADMIN + "/users/" + ID)).willReturn(aResponse().withStatus(404)));
    adapter.supprimerCompte(ID);
    keycloak.verify(deleteRequestedFor(urlEqualTo(ADMIN + "/users/" + ID)));
  }

  @Test
  void should_lister_les_comptes_cse_avec_pagination_when_synchronisation() {
    UUID autre = UUID.fromString("00000000-0000-0000-0000-000000000902");
    UUID troisieme = UUID.fromString("00000000-0000-0000-0000-000000000903");
    keycloak.stubFor(
        get(urlPathEqualTo(ADMIN + "/roles/" + "ADMIN_SYSTEME" + "/users"))
            .willReturn(okJson("[]")));
    for (Role r : Role.values()) {
      if (r != Role.CHEF_CHANTIER) {
        keycloak.stubFor(
            get(urlPathEqualTo(ADMIN + "/roles/" + r.code() + "/users")).willReturn(okJson("[]")));
      }
    }
    keycloak.stubFor(
        get(urlEqualTo(ADMIN + "/roles/CHEF_CHANTIER/users?first=0&max=2"))
            .willReturn(okJson("[{\"id\":\"" + ID + "\"},{\"id\":\"" + autre + "\"}]")));
    keycloak.stubFor(
        get(urlEqualTo(ADMIN + "/roles/CHEF_CHANTIER/users?first=2&max=2"))
            .willReturn(okJson("[{\"id\":\"" + troisieme + "\"}]")));
    for (UUID id : List.of(ID, autre, troisieme)) {
      keycloak.stubFor(
          get(urlEqualTo(ADMIN + "/users/" + id))
              .willReturn(
                  okJson(
                      "{\"id\":\""
                          + id
                          + "\",\"email\":\"c"
                          + id.toString().substring(33)
                          + "@cse.sn\",\"firstName\":\"Chef\",\"lastName\":\"Test\",\"enabled\":true,"
                          + "\"attributes\":{\"chantier_ids\":[\""
                          + CHANTIER
                          + "\",\"invalide\"],"
                          + "\"seuil_delegation\":[\"abc\"]}}")));
      keycloak.stubFor(
          get(urlEqualTo(ADMIN + "/users/" + id + "/credentials"))
              .willReturn(okJson(id.equals(ID) ? "[{\"type\":\"otp\"}]" : "[]")));
    }

    List<CompteIdentite> comptes = adapter.listerComptes();

    assertThat(comptes).hasSize(3);
    CompteIdentite premier = comptes.get(0);
    assertThat(premier.id()).isEqualTo(ID);
    assertThat(premier.roles()).containsExactly("CHEF_CHANTIER");
    assertThat(premier.otpConfigure()).isTrue();
    assertThat(premier.actif()).isTrue();
    assertThat(premier.attributs().chantierIds()).containsExactly(CHANTIER);
    assertThat(premier.attributs().seuilDelegation()).isNull();
    assertThat(comptes.get(1).otpConfigure()).isFalse();
  }
}
