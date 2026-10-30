package sn.cse.common.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Métadonnées OpenAPI ({@code cse.openapi.*}).
 *
 * @param title titre du service
 * @param description description en français
 * @param version version du contrat
 * @param keycloakUrl URL publique de Keycloak (flux OAuth2 de Swagger UI)
 * @param realm realm Keycloak
 */
@ConfigurationProperties(prefix = "cse.openapi")
public record OpenApiProperties(
    String title, String description, String version, String keycloakUrl, String realm) {

  public OpenApiProperties {
    version = version == null ? "2.0.0" : version;
    keycloakUrl = keycloakUrl == null ? "http://localhost:8180" : keycloakUrl;
    realm = realm == null ? "cse" : realm;
  }
}
