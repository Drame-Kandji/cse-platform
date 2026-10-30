package sn.cse.suivi.api.doc;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;
import sn.cse.suivi.api.dto.ProblemeDto;

/**
 * Associe le schéma {@code Probleme} (RFC 7807, {@code application/problem+json}) à toutes les
 * réponses d'erreur 4xx/5xx documentées, afin que les annotations des contrôleurs restent concises
 * et que le format d'erreur soit uniforme.
 */
@Component
public class ProblemesOpenApiCustomizer implements OpenApiCustomizer {

  /** Type de média des erreurs. */
  public static final String PROBLEM_JSON = "application/problem+json";

  private static final String SCHEMA = "Probleme";

  @Override
  public void customise(OpenAPI openApi) {
    Components components = openApi.getComponents();
    if (components == null) {
      components = new Components();
      openApi.setComponents(components);
    }
    Map<String, Schema> schemas = ModelConverters.getInstance().read(ProblemeDto.class);
    schemas.forEach(components::addSchemas);
    Content contenu =
        new Content()
            .addMediaType(
                PROBLEM_JSON,
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + SCHEMA)));
    if (openApi.getPaths() == null) {
      return;
    }
    openApi
        .getPaths()
        .values()
        .forEach(
            chemin ->
                chemin
                    .readOperations()
                    .forEach(
                        operation -> {
                          if (operation.getResponses() != null) {
                            operation
                                .getResponses()
                                .forEach((code, r) -> completer(code, r, contenu));
                          }
                        }));
  }

  private static void completer(String code, ApiResponse reponse, Content contenu) {
    if (code.startsWith("4") || code.startsWith("5")) {
      reponse.setContent(contenu);
    }
  }
}
