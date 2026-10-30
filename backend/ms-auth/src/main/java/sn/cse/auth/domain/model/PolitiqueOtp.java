package sn.cse.auth.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Politique d'authentification forte : rôles pour lesquels un second facteur TOTP est obligatoire
 * (D6 : ADMIN_SYSTEME, DIRECTION_GENERALE, DAF, RESPONSABLE_ACHATS par défaut, configurable).
 *
 * @param rolesObligatoires rôles soumis à l'OTP
 */
public record PolitiqueOtp(Set<Role> rolesObligatoires) {

  public PolitiqueOtp {
    rolesObligatoires =
        rolesObligatoires == null || rolesObligatoires.isEmpty()
            ? Set.of()
            : Set.copyOf(EnumSet.copyOf(rolesObligatoires));
  }

  /**
   * @param role rôle examiné
   * @return vrai si ce rôle doit configurer un second facteur
   */
  public boolean estObligatoire(Role role) {
    return rolesObligatoires.contains(role);
  }
}
