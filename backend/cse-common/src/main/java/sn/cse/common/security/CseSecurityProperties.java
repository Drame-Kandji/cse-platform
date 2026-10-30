package sn.cse.common.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de sécurité commune ({@code cse.security.*}).
 *
 * @param publicPaths chemins supplémentaires accessibles sans authentification
 */
@ConfigurationProperties(prefix = "cse.security")
public record CseSecurityProperties(List<String> publicPaths) {

  /** Chemins techniques toujours publics (sonde de santé, documentation OpenAPI). */
  public static final List<String> CHEMINS_TECHNIQUES =
      List.of(
          "/actuator/health/**",
          "/actuator/info",
          "/actuator/prometheus",
          "/swagger-ui.html",
          "/swagger-ui/**",
          "/v3/api-docs/**",
          "/error");

  public CseSecurityProperties {
    publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
  }

  /**
   * @return chemins techniques + chemins configurés
   */
  public String[] tousLesCheminsPublics() {
    List<String> tous = new ArrayList<>(CHEMINS_TECHNIQUES);
    tous.addAll(publicPaths);
    return tous.toArray(String[]::new);
  }
}
