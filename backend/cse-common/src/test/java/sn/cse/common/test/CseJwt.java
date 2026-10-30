package sn.cse.common.test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import sn.cse.common.security.KeycloakJwtConverter;

/**
 * Jetons JWT de test reproduisant ceux de Keycloak (rôles realm, chantiers, fournisseur), à
 * utiliser avec MockMvc : {@code mvc.perform(get(...).with(CseJwt.as(UTILISATEUR, "DAF")))}.
 */
public final class CseJwt {

  private CseJwt() {}

  /**
   * @param userId identifiant ({@code sub})
   * @param roles rôles realm
   * @return post-processeur MockMvc
   */
  public static JwtRequestPostProcessor as(UUID userId, String... roles) {
    return avecClaims(userId, Map.of(), roles);
  }

  /**
   * @param userId identifiant du chef de chantier
   * @param chantiers chantiers attribués
   * @return post-processeur MockMvc pour un chef de chantier
   */
  public static JwtRequestPostProcessor chef(UUID userId, UUID... chantiers) {
    List<String> ids = Arrays.stream(chantiers).map(UUID::toString).toList();
    return avecClaims(userId, Map.of("chantier_ids", ids), "CHEF_CHANTIER");
  }

  /**
   * @param userId identifiant
   * @param claims claims supplémentaires
   * @param roles rôles realm
   * @return post-processeur MockMvc
   */
  public static JwtRequestPostProcessor avecClaims(
      UUID userId, Map<String, Object> claims, String... roles) {
    return jwt()
        .jwt(
            j -> {
              j.subject(userId.toString())
                  .claim("name", "Utilisateur Test")
                  .claim("email", "test@cse.sn")
                  .claim("realm_access", Map.of("roles", List.of(roles)));
              claims.forEach(j::claim);
            })
        .authorities(KeycloakJwtConverter::extraireAutorites);
  }
}
