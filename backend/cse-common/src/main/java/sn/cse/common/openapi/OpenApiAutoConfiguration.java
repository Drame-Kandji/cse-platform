package sn.cse.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Document OpenAPI commun : informations du service, schéma {@code bearerAuth} (JWT) et flux OAuth2
 * Authorization Code + PKCE pour obtenir un jeton directement depuis Swagger UI.
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@EnableConfigurationProperties(OpenApiProperties.class)
public class OpenApiAutoConfiguration {

  static final String BEARER = "bearerAuth";
  static final String OAUTH = "keycloak";

  /**
   * @param props métadonnées
   * @param nomService nom du service
   * @return la description OpenAPI
   */
  @Bean
  public OpenAPI cseOpenApi(
      OpenApiProperties props, @Value("${spring.application.name}") String nomService) {
    String base = props.keycloakUrl() + "/realms/" + props.realm() + "/protocol/openid-connect";
    return new OpenAPI()
        .info(
            new Info()
                .title(props.title() == null ? nomService : props.title())
                .description(props.description())
                .version(props.version())
                .contact(new Contact().name("Équipe plateforme CSE").email("support@cse.sn")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Jeton d'accès Keycloak (realm cse)"))
                .addSecuritySchemes(
                    OAUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(
                            new OAuthFlows()
                                .authorizationCode(
                                    new OAuthFlow()
                                        .authorizationUrl(base + "/auth")
                                        .tokenUrl(base + "/token")))))
        .addSecurityItem(new SecurityRequirement().addList(BEARER))
        .addSecurityItem(new SecurityRequirement().addList(OAUTH));
  }
}
