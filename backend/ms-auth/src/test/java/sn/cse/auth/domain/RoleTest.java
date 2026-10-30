package sn.cse.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import sn.cse.auth.domain.model.Role;

class RoleTest {

  @Test
  void should_retrouver_le_role_when_code_cse() {
    assertThat(Role.depuisCode("DAF")).contains(Role.DAF);
    assertThat(Role.depuisCode("default-roles-cse")).isEmpty();
    assertThat(Role.CHEF_CHANTIER.code()).isEqualTo("CHEF_CHANTIER");
  }

  @Test
  void should_choisir_le_role_prioritaire_when_plusieurs_roles() {
    assertThat(Role.principal(List.of("default-roles-cse", "CHEF_CHANTIER", "ADMIN_SYSTEME")))
        .contains(Role.ADMIN_SYSTEME);
    assertThat(Role.principal(List.of("offline_access"))).isEmpty();
  }
}
