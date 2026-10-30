package sn.cse.common.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Accès à l'utilisateur courant depuis le contexte de sécurité. Utilisé par les contrôleurs (pour
 * construire les commandes) et par l'outbox (pour renseigner l'auteur des événements).
 */
public class CurrentUserProvider {

  static final String CLAIM_CHANTIERS = "chantier_ids";
  static final String CLAIM_FOURNISSEUR = "fournisseur_id";

  /**
   * @return l'utilisateur authentifié par JWT, s'il existe
   */
  public Optional<CurrentUser> current() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken token) {
      return Optional.of(depuisJwt(token.getToken()));
    }
    return Optional.empty();
  }

  /**
   * @return l'utilisateur authentifié
   * @throws IllegalStateException si aucun JWT n'est présent (ne devrait pas arriver derrière la
   *     chaîne de sécurité)
   */
  public CurrentUser require() {
    return current().orElseThrow(() -> new IllegalStateException("Aucun utilisateur authentifié"));
  }

  /**
   * @return la valeur brute du jeton courant, pour le relais vers d'autres services
   */
  public Optional<String> currentToken() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken token) {
      return Optional.of(token.getToken().getTokenValue());
    }
    return Optional.empty();
  }

  /**
   * @param jwt jeton Keycloak
   * @return l'utilisateur correspondant
   */
  public static CurrentUser depuisJwt(Jwt jwt) {
    return new CurrentUser(
        UUID.fromString(jwt.getSubject()),
        nom(jwt),
        jwt.getClaimAsString("email"),
        new HashSet<>(KeycloakJwtConverter.rolesRealm(jwt)),
        uuids(jwt.getClaims().get(CLAIM_CHANTIERS)),
        Optional.ofNullable(jwt.getClaimAsString(CLAIM_FOURNISSEUR))
            .filter(s -> !s.isBlank())
            .map(UUID::fromString)
            .orElse(null));
  }

  private static String nom(Jwt jwt) {
    String name = jwt.getClaimAsString("name");
    if (name != null && !name.isBlank()) {
      return name;
    }
    String prenom = Optional.ofNullable(jwt.getClaimAsString("given_name")).orElse("");
    String famille = Optional.ofNullable(jwt.getClaimAsString("family_name")).orElse("");
    String complet = (prenom + " " + famille).trim();
    return complet.isEmpty() ? jwt.getClaimAsString("preferred_username") : complet;
  }

  // Keycloak émet un attribut multivalué sous forme de liste, et un attribut simple sous forme de
  // chaîne : les deux formes sont acceptées.
  private static Set<UUID> uuids(Object claim) {
    if (claim instanceof Collection<?> valeurs) {
      return valeurs.stream()
          .map(String::valueOf)
          .filter(s -> !s.isBlank())
          .map(UUID::fromString)
          .collect(Collectors.toSet());
    }
    if (claim instanceof String s && !s.isBlank()) {
      return Set.of(UUID.fromString(s));
    }
    return Set.of();
  }
}
