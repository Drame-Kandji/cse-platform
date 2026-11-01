package sn.cse.gateway.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import sn.cse.common.security.KeycloakJwtConverter;
import sn.cse.common.security.Roles;
import sn.cse.gateway.config.GatewayProperties;
import sn.cse.gateway.filter.CorrelationIdWebFilter;
import sn.cse.gateway.web.Problemes;

/**
 * Sécurité réactive de la passerelle : toute route {@code /api/**} exige un JWT Keycloak valide
 * (signature JWKS + émetteur), sauf les routes publiques déclarées par service ({@code
 * cse.gateway.services.<service>.publiques}) ; le portail exige le rôle FOURNISSEUR ; les services
 * revérifient les rôles. API sans état : pas de session, CSRF désactivé (jeton porté par en-tête,
 * ADR-0006).
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class SecurityConfiguration {

  /** Chemins techniques publics (santé, documentation agrégée). */
  static final String[] CHEMINS_PUBLICS = {
    "/actuator/health",
    "/actuator/health/**",
    "/actuator/info",
    "/actuator/prometheus",
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/webjars/**",
    "/v3/api-docs/**"
  };

  static final String PORTAIL = "/api/portail/**";
  static final String PLATEFORME = "/api/plateforme/**";
  static final String API = "/api/**";

  /**
   * @param http configurateur
   * @param properties configuration de la passerelle
   * @param problemes erreurs RFC 7807
   * @return la chaîne de filtres de sécurité
   */
  @Bean
  public SecurityWebFilterChain cseSecurityWebFilterChain(
      ServerHttpSecurity http, GatewayProperties properties, Problemes problemes) {
    ServerAuthenticationEntryPoint nonAuthentifie =
        (exchange, ex) ->
            problemes.ecrire(
                exchange.getResponse(),
                problemes.creer(
                    HttpStatus.UNAUTHORIZED,
                    "NON_AUTHENTIFIE",
                    "Authentification requise",
                    "Jeton d'accès absent, invalide ou expiré.",
                    exchange));
    ServerAccessDeniedHandler refuse =
        (exchange, ex) ->
            problemes.ecrire(
                exchange.getResponse(),
                problemes.creer(
                    HttpStatus.FORBIDDEN,
                    "ACCES_REFUSE",
                    "Accès refusé",
                    "Votre rôle ne permet pas cette opération.",
                    exchange));
    List<String> portailOuvert = properties.securite().portailSansRoleFournisseur();
    String[] internes = Roles.INTERNES.toArray(String[]::new);

    http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable)
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        .cors(Customizer.withDefaults())
        .headers(
            h ->
                h.frameOptions(f -> f.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                    .referrerPolicy(
                        r ->
                            r.policy(
                                ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.NO_REFERRER))
                    .permissionsPolicy(
                        p -> p.policy("geolocation=(), camera=(), microphone=(), payment=()")))
        .authorizeExchange(
            a -> {
              a.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();
              a.pathMatchers(CHEMINS_PUBLICS).permitAll();
              // Routes publiques (ex. POST /api/portail/inscription) : méthode et chemin exacts,
              // évaluées avant la règle FOURNISSEUR du portail.
              properties
                  .routesPubliques()
                  .values()
                  .forEach(
                      liste ->
                          liste.forEach(
                              p -> a.pathMatchers(p.methodeHttp(), p.chemin()).permitAll()));
              if (!portailOuvert.isEmpty()) {
                a.pathMatchers(portailOuvert.toArray(String[]::new)).authenticated();
              }
              a.pathMatchers(PORTAIL).hasRole(Roles.FOURNISSEUR);
              a.pathMatchers(PLATEFORME).hasAnyRole(internes);
              a.pathMatchers(API).authenticated();
              a.anyExchange().denyAll();
            })
        .oauth2ResourceServer(
            o ->
                o.jwt(
                        j ->
                            j.jwtAuthenticationConverter(
                                new ReactiveJwtAuthenticationConverterAdapter(
                                    new KeycloakJwtConverter())))
                    .authenticationEntryPoint(nonAuthentifie)
                    .accessDeniedHandler(refuse))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(nonAuthentifie).accessDeniedHandler(refuse));
    return http.build();
  }

  /**
   * CORS : origines exactes configurées, identifiants autorisés, corrélation exposée.
   *
   * @param properties configuration
   * @return la source de configuration CORS
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource(GatewayProperties properties) {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(properties.cors().originesAutorisees());
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(
        List.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.ACCEPT,
            HttpHeaders.CACHE_CONTROL,
            CorrelationIdWebFilter.HEADER,
            "Last-Event-ID"));
    cors.setExposedHeaders(
        List.of(
            CorrelationIdWebFilter.HEADER,
            HttpHeaders.LOCATION,
            HttpHeaders.CONTENT_DISPOSITION,
            HttpHeaders.RETRY_AFTER,
            "X-RateLimit-Remaining"));
    cors.setAllowCredentials(true);
    cors.setMaxAge(properties.cors().maxAge());
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return source;
  }
}
