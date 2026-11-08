package sn.cse.achats.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Contrat OpenAPI figé (instantané {@code src/test/resources/openapi.json}). Régénérer
 * volontairement l'instantané avec {@code -Dcse.openapi.regenerer=true}.
 */
class ContratApiIntegrationTest extends IntegrationTestBase {

  private static final Path INSTANTANE = Path.of("src/test/resources/openapi.json");

  @Test
  void should_exposer_le_contrat_openapi_fige() throws Exception {
    String brut =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    ObjectNode actuel = (ObjectNode) mapper.readTree(brut);
    // L'URL du serveur dépend de l'environnement d'exécution.
    actuel.remove("servers");
    String normalise =
        mapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(actuel) + "\n";
    if (Boolean.getBoolean("cse.openapi.regenerer")) {
      Files.writeString(INSTANTANE, normalise, StandardCharsets.UTF_8);
    }
    assertThat(INSTANTANE)
        .as("Instantané OpenAPI absent : lancer une fois avec -Dcse.openapi.regenerer=true")
        .exists();
    JsonNode attendu = mapper.readTree(Files.readString(INSTANTANE, StandardCharsets.UTF_8));
    assertThat(actuel).isEqualTo(attendu);
    assertThat(
            actuel
                .at(
                    "/paths/~1api~1appels-offres~1{id}~1proces-verbaux~1{type}/get/responses/200/content")
                .has("application/pdf"))
        .isTrue();
    assertThat(actuel.at("/components/schemas/ProblemDetail").isMissingNode()).isFalse();
  }
}
