package sn.cse.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Chaîne de sécurité commune aux microservices servlet : API sans état protégée par JWT Keycloak,
 * RBAC par {@code @PreAuthorize}, erreurs 401/403 en ProblemDetail.
 *
 * <p>CSRF est désactivé volontairement : l'API est sans session et authentifiée uniquement par
 * en-tête {@code Authorization: Bearer}, qu'un site tiers ne peut pas forger (ADR-0006).
 */
@AutoConfiguration(
    before = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity
@EnableConfigurationProperties(CseSecurityProperties.class)
public class CseSecurityAutoConfiguration {

  /**
   * @return le fournisseur d'utilisateur courant
   */
  @Bean
  @ConditionalOnMissingBean
  public CurrentUserProvider currentUserProvider() {
    return new CurrentUserProvider();
  }

  /**
   * @param http configurateur Spring Security
   * @param properties chemins publics
   * @param mapper sérialiseur JSON pour les erreurs
   * @return la chaîne de filtres
   * @throws Exception erreur de configuration
   */
  @Bean
  @ConditionalOnMissingBean(SecurityFilterChain.class)
  public SecurityFilterChain cseSecurityFilterChain(
      HttpSecurity http, CseSecurityProperties properties, ObjectMapper mapper) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(properties.tousLesCheminsPublics())
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth ->
                oauth
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtConverter()))
                    .authenticationEntryPoint(ProblemDetailSecurityHandlers.entryPoint(mapper))
                    .accessDeniedHandler(ProblemDetailSecurityHandlers.accessDenied(mapper)))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(ProblemDetailSecurityHandlers.entryPoint(mapper))
                    .accessDeniedHandler(ProblemDetailSecurityHandlers.accessDenied(mapper)));
    return http.build();
  }
}
