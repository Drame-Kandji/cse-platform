package sn.cse.common.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Convertit un JWT Keycloak en authentification Spring : les rôles du claim {@code
 * realm_access.roles} deviennent des autorités {@code ROLE_<ROLE>}, ce qui rend utilisables les
 * expressions {@code hasRole('DAF')}. Le convertisseur par défaut de Spring ne lit que {@code
 * scope}.
 */
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  static final String REALM_ACCESS = "realm_access";
  static final String ROLES = "roles";

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    return new JwtAuthenticationToken(jwt, extraireAutorites(jwt), jwt.getSubject());
  }

  /**
   * @param jwt jeton décodé
   * @return autorités {@code ROLE_*} issues des rôles realm
   */
  public static Collection<GrantedAuthority> extraireAutorites(Jwt jwt) {
    return rolesRealm(jwt).stream()
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(Roles.PREFIX + role))
        .toList();
  }

  /**
   * @param jwt jeton décodé
   * @return rôles realm (liste vide si le claim est absent)
   */
  public static List<String> rolesRealm(Jwt jwt) {
    Object realmAccess = jwt.getClaims().get(REALM_ACCESS);
    if (realmAccess instanceof Map<?, ?> map && map.get(ROLES) instanceof Collection<?> roles) {
      return roles.stream().map(String::valueOf).toList();
    }
    return List.of();
  }
}
