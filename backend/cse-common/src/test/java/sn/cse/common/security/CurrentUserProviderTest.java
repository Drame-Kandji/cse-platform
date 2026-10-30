package sn.cse.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class CurrentUserProviderTest {

  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID CHANTIER = UUID.fromString("c0000000-0000-0000-0000-000000000001");

  private static Jwt jwt(Map<String, Object> claims) {
    Jwt.Builder b =
        Jwt.withTokenValue("t")
            .header("alg", "RS256")
            .subject(ID.toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60));
    claims.forEach(b::claim);
    return b.build();
  }

  @Test
  void should_map_realm_roles_to_spring_authorities() {
    Jwt token = jwt(Map.of("realm_access", Map.of("roles", List.of("DAF", "offline_access"))));

    assertThat(KeycloakJwtConverter.extraireAutorites(token))
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_DAF", "ROLE_offline_access");
  }

  @Test
  void should_build_chef_restricted_to_his_sites() {
    CurrentUser user =
        CurrentUserProvider.depuisJwt(
            jwt(
                Map.of(
                    "given_name",
                    "Mamadou",
                    "family_name",
                    "Diop",
                    "realm_access",
                    Map.of("roles", List.of(Roles.CHEF_CHANTIER)),
                    "chantier_ids",
                    List.of(CHANTIER.toString()))));

    assertThat(user.nom()).isEqualTo("Mamadou Diop");
    assertThat(user.estRestreintAuxChantiers()).isTrue();
    assertThat(user.peutAccederAuChantier(CHANTIER)).isTrue();
    assertThat(user.peutAccederAuChantier(UUID.randomUUID())).isFalse();
  }

  @Test
  void should_not_restrict_responsable_materiel() {
    CurrentUser user =
        CurrentUserProvider.depuisJwt(
            jwt(Map.of("realm_access", Map.of("roles", List.of(Roles.RESPONSABLE_MATERIEL)))));

    assertThat(user.estRestreintAuxChantiers()).isFalse();
    assertThat(user.peutAccederAuChantier(UUID.randomUUID())).isTrue();
    assertThat(user.fournisseur()).isEmpty();
  }

  @Test
  void should_accept_single_valued_attribute_claims() {
    UUID fournisseur = UUID.randomUUID();
    CurrentUser user =
        CurrentUserProvider.depuisJwt(
            jwt(
                Map.of(
                    "chantier_ids", CHANTIER.toString(),
                    "fournisseur_id", fournisseur.toString())));

    assertThat(user.chantierIds()).containsExactly(CHANTIER);
    assertThat(user.fournisseur()).contains(fournisseur);
  }
}
