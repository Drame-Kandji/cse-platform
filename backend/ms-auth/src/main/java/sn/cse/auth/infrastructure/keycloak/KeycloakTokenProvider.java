package sn.cse.auth.infrastructure.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Jeton d'accès du compte de service (grant {@code client_credentials}), mis en cache jusqu'à peu
 * avant son expiration. Thread-safe.
 */
public class KeycloakTokenProvider {

  private final RestClient http;
  private final KeycloakProperties properties;
  private final Clock clock;

  private String jeton;
  private Instant expiration = Instant.MIN;

  /**
   * @param http client HTTP
   * @param properties paramètres Keycloak
   * @param clock horloge
   */
  public KeycloakTokenProvider(RestClient http, KeycloakProperties properties, Clock clock) {
    this.http = http;
    this.properties = properties;
    this.clock = clock;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ReponseJeton(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("expires_in") long expiresIn) {}

  /**
   * @return un jeton valide (renouvelé si nécessaire)
   */
  public synchronized String jeton() {
    Instant maintenant = Instant.now(clock);
    if (jeton == null || !maintenant.isBefore(expiration)) {
      MultiValueMap<String, String> formulaire = new LinkedMultiValueMap<>();
      formulaire.add("grant_type", "client_credentials");
      formulaire.add("client_id", properties.clientId());
      formulaire.add(
          "client_secret", properties.clientSecret() == null ? "" : properties.clientSecret());
      ReponseJeton reponse =
          http.post()
              .uri(properties.tokenUri())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(formulaire)
              .retrieve()
              .body(ReponseJeton.class);
      if (reponse == null || reponse.accessToken() == null) {
        throw new IllegalStateException("Réponse de jeton Keycloak vide");
      }
      jeton = reponse.accessToken();
      expiration =
          maintenant.plusSeconds(reponse.expiresIn()).minus(properties.margeExpirationJeton());
    }
    return jeton;
  }

  /** Oublie le jeton courant (révoqué ou refusé par Keycloak). */
  public synchronized void invalider() {
    jeton = null;
    expiration = Instant.MIN;
  }
}
