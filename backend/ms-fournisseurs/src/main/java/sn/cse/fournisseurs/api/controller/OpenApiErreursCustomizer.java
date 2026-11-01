package sn.cse.fournisseurs.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.method.HandlerMethod;

/**
 * Documente les réponses d'erreur RFC 7807 ({@code application/problem+json}) de chaque opération :
 * 401/403 partout, 400 et 404 selon les paramètres, 409/422 pour les opérations d'écriture.
 */
@Component
public class OpenApiErreursCustomizer implements OperationCustomizer, OpenApiCustomizer {

  private static final String SCHEMA_PROBLEME = "ProblemDetail";

  private static final String PROBLEM_JSON = "application/problem+json";
  private static final Map<String, String[]> ERREURS = new LinkedHashMap<>();

  static {
    ERREURS.put(
        "400",
        new String[] {
          "Requête invalide", "VALIDATION", "Certains champs du formulaire sont invalides."
        });
    ERREURS.put(
        "401",
        new String[] {
          "Authentification requise", "NON_AUTHENTIFIE", "Jeton d'accès absent, invalide ou expiré."
        });
    ERREURS.put(
        "403",
        new String[] {"Accès refusé", "ACCES_REFUSE", "Votre rôle ne permet pas cette opération."});
    ERREURS.put(
        "404",
        new String[] {
          "Ressource introuvable",
          "FOURNISSEUR_INTROUVABLE",
          "Fournisseur introuvable : f0000000-0000-0000-0000-000000000099"
        });
    ERREURS.put(
        "409",
        new String[] {
          "Conflit d'état ou de concurrence",
          "TRANSITION_STATUT_INVALIDE",
          "Transition impossible pour ce fournisseur : ACTIF → BLACKLISTE."
        });
    ERREURS.put(
        "422",
        new String[] {
          "Règle de gestion non respectée", "MOTIF_OBLIGATOIRE", "Le motif est obligatoire."
        });
  }

  @Override
  public Operation customize(Operation operation, HandlerMethod handlerMethod) {
    boolean lecture = handlerMethod.hasMethodAnnotation(GetMapping.class);
    boolean cheminAvecId =
        operation.getParameters() != null
            && operation.getParameters().stream().anyMatch(p -> "path".equals(p.getIn()));
    ApiResponses reponses =
        operation.getResponses() == null ? new ApiResponses() : operation.getResponses();
    ajouter(reponses, "400");
    // Une opération publique (@SecurityRequirements vide) ne renvoie ni 401 ni 403.
    if (!handlerMethod.hasMethodAnnotation(SecurityRequirements.class)) {
      ajouter(reponses, "401");
      ajouter(reponses, "403");
    }
    if (cheminAvecId) {
      ajouter(reponses, "404");
    }
    if (!lecture) {
      ajouter(reponses, "409");
      ajouter(reponses, "422");
    }
    operation.setResponses(reponses);
    return operation;
  }

  @Override
  public void customise(OpenAPI openApi) {
    if (openApi.getComponents() == null) {
      openApi.setComponents(new Components());
    }
    ObjectSchema erreurChamp = new ObjectSchema();
    erreurChamp.addProperty("field", new StringSchema().example("motif"));
    erreurChamp.addProperty("message", new StringSchema().example("ne doit pas être vide"));
    ObjectSchema probleme = new ObjectSchema();
    probleme.description("Erreur RFC 7807 (application/problem+json)");
    probleme.addProperty("type", new StringSchema().example("about:blank"));
    probleme.addProperty("title", new StringSchema().example("Conflit"));
    probleme.addProperty("status", new IntegerSchema().example(409));
    probleme.addProperty("detail", new StringSchema().example("Le motif est obligatoire."));
    probleme.addProperty("code", new StringSchema().example("MOTIF_OBLIGATOIRE"));
    probleme.addProperty("traceId", new StringSchema().example("4bf92f3577b34da6a3ce929d0e0e4736"));
    probleme.addProperty("timestamp", new StringSchema().example("2026-10-30T09:15:00Z"));
    probleme.addProperty("errors", new ArraySchema().items(erreurChamp));
    openApi.getComponents().addSchemas(SCHEMA_PROBLEME, probleme);
  }

  private static void ajouter(ApiResponses reponses, String code) {
    if (reponses.containsKey(code)) {
      return;
    }
    String[] e = ERREURS.get(code);
    Map<String, Object> exemple = new LinkedHashMap<>();
    exemple.put("type", "about:blank");
    exemple.put("title", e[0]);
    exemple.put("status", Integer.valueOf(code));
    exemple.put("detail", e[2]);
    exemple.put("code", e[1]);
    exemple.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
    exemple.put("timestamp", "2026-10-30T09:15:00Z");
    reponses.addApiResponse(
        code,
        new ApiResponse()
            .description(e[0])
            .content(
                new Content()
                    .addMediaType(
                        PROBLEM_JSON,
                        new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/" + SCHEMA_PROBLEME))
                            .example(exemple))));
  }
}
