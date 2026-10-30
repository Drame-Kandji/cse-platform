package sn.cse.gateway.config;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI agrégé : une entrée par service ({@code /v3/api-docs/<service>}, relayé et réécrit par
 * la passerelle) plus la documentation propre de la passerelle.
 */
@Configuration(proxyBeanMethods = false)
public class DocumentationConfiguration {

  /**
   * @param swaggerUi configuration Swagger UI de springdoc
   * @param properties services routés
   * @param documentationPasserelle chemin de la documentation de la passerelle
   * @param nom nom de la passerelle
   */
  public DocumentationConfiguration(
      SwaggerUiConfigProperties swaggerUi,
      GatewayProperties properties,
      @Value("${springdoc.api-docs.path}") String documentationPasserelle,
      @Value("${spring.application.name}") String nom) {
    Set<SwaggerUrl> urls = new LinkedHashSet<>();
    properties
        .services()
        .forEach(
            (service, config) -> {
              if (Boolean.TRUE.equals(config.documentation())) {
                urls.add(url(service, RoutesConfiguration.PREFIXE_DOCUMENTATION + service));
              }
            });
    urls.add(url(nom, documentationPasserelle));
    swaggerUi.setUrls(urls);
    urls.stream().findFirst().ifPresent(premier -> swaggerUi.setUrlsPrimaryName(premier.getName()));
  }

  private static SwaggerUrl url(String nom, String chemin) {
    SwaggerUrl url = new SwaggerUrl();
    url.setName(nom);
    url.setDisplayName(nom);
    url.setUrl(chemin);
    return url;
  }
}
