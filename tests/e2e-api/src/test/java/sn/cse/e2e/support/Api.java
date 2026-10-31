package sn.cse.e2e.support;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Accès à la passerelle sous l'identité d'un compte : jeton Keycloak obtenu par password grant avec
 * le client de développement {@code cse-tests-e2e}, mis en cache jusqu'à 30 s avant expiration.
 */
public final class Api {

  private static final Map<Compte, Jeton> JETONS = new EnumMap<>(Compte.class);

  private Api() {}

  private record Jeton(String valeur, Instant expireLe) {}

  /**
   * Requête authentifiée (JSON, corrélation) vers la passerelle.
   *
   * @param compte compte appelant
   * @return spécification RestAssured prête à l'emploi
   */
  public static RequestSpecification en(Compte compte) {
    return anonyme().auth().oauth2(jeton(compte));
  }

  /**
   * Requête sans jeton (vérification du 401).
   *
   * @return spécification RestAssured
   */
  public static RequestSpecification anonyme() {
    return given()
        .baseUri(E2eConfig.gateway())
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .header("X-Correlation-Id", "e2e-" + UUID.randomUUID());
  }

  /**
   * Vérifie le statut puis renvoie la réponse (message d'échec avec le corps ProblemDetail).
   *
   * @param reponse réponse HTTP
   * @param statut statut attendu
   * @return la réponse, pour extraction
   */
  public static Response attendu(Response reponse, int statut) {
    assertThat(reponse.statusCode())
        .as("HTTP %s attendu, corps : %s", statut, reponse.asString())
        .isEqualTo(statut);
    return reponse;
  }

  private static synchronized String jeton(Compte compte) {
    Jeton enCache = JETONS.get(compte);
    if (enCache != null && Instant.now().plusSeconds(30).isBefore(enCache.expireLe())) {
      return enCache.valeur();
    }
    Response reponse =
        given()
            .contentType(ContentType.URLENC)
            .formParam("client_id", E2eConfig.client())
            .formParam("grant_type", "password")
            .formParam("username", compte.email())
            .formParam("password", E2eConfig.motDePasse())
            .post(E2eConfig.tokenEndpoint());
    attendu(reponse, 200);
    Jeton jeton =
        new Jeton(
            reponse.path("access_token"),
            Instant.now().plus(Duration.ofSeconds(reponse.<Integer>path("expires_in"))));
    JETONS.put(compte, jeton);
    return jeton.valeur();
  }
}
