package sn.cse.achats.api.config;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.cse.achats.api.dto.ProblemeDto;

/**
 * Documente sur chaque opération les réponses d'erreur RFC 7807 (schéma {@code ProblemDetail},
 * exemples en français) sans les répéter dans chaque contrôleur.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiErreursConfiguration {

  static final String SCHEMA = "ProblemDetail";
  private static final String PROBLEM_JSON = "application/problem+json";

  private record Erreur(String description, String code, String detail) {}

  private static final Map<String, Erreur> ERREURS = new LinkedHashMap<>();

  static {
    ERREURS.put(
        "400",
        new Erreur(
            "Requête invalide (validation des champs ou paramètres)",
            "VALIDATION",
            "Certains champs du formulaire sont invalides."));
    ERREURS.put(
        "401",
        new Erreur(
            "Jeton absent, invalide ou expiré",
            "NON_AUTHENTIFIE",
            "Jeton d'accès absent, invalide ou expiré."));
    ERREURS.put(
        "403",
        new Erreur(
            "Rôle insuffisant", "ACCES_REFUSE", "Votre rôle ne permet pas cette opération."));
    ERREURS.put(
        "404",
        new Erreur(
            "Ressource introuvable",
            "APPELOFFRE_INTROUVABLE",
            "AppelOffre introuvable : d0000000-0000-0000-0000-000000000099"));
    ERREURS.put(
        "409",
        new Erreur(
            "Conflit d'état ou de concurrence",
            "TRANSITION_INTERDITE",
            "Transition interdite du contrat : BROUILLON → EN_LIVRAISON."));
    ERREURS.put(
        "422",
        new Erreur(
            "Règle de gestion non respectée",
            "DATE_LIMITE_DEPASSEE",
            "La date limite de dépôt des offres est dépassée (R02)."));
  }

  /**
   * @return le personnalisateur ajoutant les réponses d'erreur standard
   */
  @Bean
  public OpenApiCustomizer erreursStandard() {
    return openApi -> {
      Schema<?> schema =
          ModelConverters.getInstance()
              .resolveAsResolvedSchema(new AnnotatedType(ProblemeDto.class))
              .schema;
      if (openApi.getComponents() == null) {
        openApi.setComponents(new Components());
      }
      openApi.getComponents().addSchemas(SCHEMA, schema);
      if (openApi.getPaths() == null) {
        return;
      }
      openApi
          .getPaths()
          .forEach(
              (chemin, item) ->
                  item.readOperations().forEach(op -> completer(op, chemin.contains("{"))));
    };
  }

  private static void completer(Operation operation, boolean avecIdentifiant) {
    ApiResponses reponses = operation.getResponses();
    ERREURS.forEach(
        (statut, erreur) -> {
          if (!avecIdentifiant && "404".equals(statut)) {
            return;
          }
          if (reponses.containsKey(statut)) {
            return;
          }
          Map<String, Object> exemple = new LinkedHashMap<>();
          exemple.put(
              "type",
              "https://cse.sn/erreurs/"
                  + erreur.code().toLowerCase(java.util.Locale.ROOT).replace('_', '-'));
          exemple.put("title", erreur.description());
          exemple.put("status", Integer.valueOf(statut));
          exemple.put("detail", erreur.detail());
          exemple.put("code", erreur.code());
          exemple.put("traceId", "3f6c1a2b-9d8e-4f70-b1a2-c3d4e5f60718");
          exemple.put("timestamp", "2026-10-30T09:15:00Z");
          MediaType media =
              new MediaType()
                  .schema(new Schema<>().$ref("#/components/schemas/" + SCHEMA))
                  .addExamples(erreur.code(), new Example().value(exemple));
          reponses.addApiResponse(
              statut,
              new ApiResponse()
                  .description(erreur.description())
                  .content(new Content().addMediaType(PROBLEM_JSON, media)));
        });
  }
}
