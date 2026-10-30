package sn.cse.auth.infrastructure.keycloak;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Accès à l'API d'administration Keycloak ({@code cse.auth.keycloak.*}) via le compte de service
 * {@code cse-admin-api} (grant client_credentials).
 *
 * @param url URL de base de Keycloak (ex. {@code http://localhost:8180})
 * @param realm realm administré
 * @param clientId client confidentiel du compte de service
 * @param clientSecret secret du client (variable d'environnement, jamais commité)
 * @param connectTimeout délai de connexion
 * @param readTimeout délai de lecture
 * @param margeExpirationJeton marge avant expiration à partir de laquelle le jeton est renouvelé
 * @param taillePage taille des pages lors du parcours des comptes
 */
@Validated
@ConfigurationProperties(prefix = "cse.auth.keycloak")
public record KeycloakProperties(
    @NotBlank String url,
    @NotBlank String realm,
    @NotBlank String clientId,
    String clientSecret,
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout,
    @NotNull Duration margeExpirationJeton,
    @Min(1) @Max(1000) int taillePage) {

  /**
   * @return URL du point de jeton OpenID Connect
   */
  public String tokenUri() {
    return url + "/realms/" + realm + "/protocol/openid-connect/token";
  }

  /**
   * @return préfixe des ressources d'administration du realm
   */
  public String adminBase() {
    return url + "/admin/realms/" + realm;
  }
}
