package sn.cse.e2e.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/** Vérifie la forme RFC 7807 imposée par le contrat d'API §0 ({@code application/problem+json}). */
public final class ProblemDetailAssert {

  private ProblemDetailAssert() {}

  /**
   * Contrôle statut, type de contenu, champs obligatoires et code métier d'une erreur.
   *
   * @param reponse réponse HTTP en erreur
   * @param statut statut HTTP attendu
   * @param code code métier attendu (ex. {@code JUSTIFICATION_OBLIGATOIRE})
   * @return le corps, pour des vérifications complémentaires
   */
  public static JsonPath problemDetail(Response reponse, int statut, String code) {
    Api.attendu(reponse, statut);
    assertThat(reponse.contentType()).startsWith("application/problem+json");
    JsonPath corps = reponse.jsonPath();
    assertThat(corps.getInt("status")).isEqualTo(statut);
    assertThat(corps.getString("code")).isEqualTo(code);
    assertThat(corps.getString("type")).startsWith("https://");
    assertThat(corps.getString("title")).isNotBlank();
    assertThat(corps.getString("detail")).isNotBlank();
    assertThat(corps.getString("traceId")).isNotBlank();
    assertThat(corps.getString("timestamp")).isNotBlank();
    // Jamais de trace technique exposée au client (AGENTS.md A.2).
    assertThat(reponse.asString()).doesNotContain("Exception").doesNotContain("at sn.cse.");
    return corps;
  }
}
