package sn.cse.suivi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrat OpenAPI figé : toute évolution de {@code /v3/api-docs} doit être volontaire. Pour
 * régénérer le fichier de référence : {@code -Dopenapi.regenerer=true}.
 */
class OpenApiSnapshotTest extends IntegrationTestSupport {

  private static final Path SNAPSHOT = Path.of("src/test/resources/openapi.json");

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;

  @Test
  void should_match_published_openapi_contract() throws Exception {
    String brut =
        mvc.perform(get("/v3/api-docs"))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode actuel = mapper.readTree(brut);
    // Le serveur dépend de l'environnement d'exécution : il est exclu de la comparaison.
    ((ObjectNode) actuel).remove("servers");

    if (Boolean.getBoolean("openapi.regenerer")) {
      Files.writeString(
          SNAPSHOT, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actuel) + "\n");
    }

    assertThat(SNAPSHOT)
        .as("Snapshot OpenAPI absent : lancer avec -Dopenapi.regenerer=true")
        .exists();
    assertThat(actuel).isEqualTo(mapper.readTree(SNAPSHOT.toFile()));
    assertThat(
            actuel
                .at("/paths/~1api~1materiels~1{id}~1affecter/post/responses/409/content")
                .isMissingNode())
        .isFalse();
  }
}
