package sn.cse.auth.domain.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import sn.cse.common.security.Roles;

/**
 * Rôles métier de la plateforme CSE. Chaque rôle correspond à exactement un rôle realm Keycloak et
 * à une sous-classe concrète de {@link Utilisateur}. L'ordre de déclaration sert de priorité
 * lorsqu'un compte Keycloak porte plusieurs rôles CSE (cas anormal toléré à la synchronisation).
 */
public enum Role {
  ADMIN_SYSTEME(Roles.ADMIN_SYSTEME),
  DIRECTION_GENERALE(Roles.DIRECTION_GENERALE),
  DAF(Roles.DAF),
  RESPONSABLE_ACHATS(Roles.RESPONSABLE_ACHATS),
  RESPONSABLE_MATERIEL(Roles.RESPONSABLE_MATERIEL),
  CHEF_CHANTIER(Roles.CHEF_CHANTIER),
  FOURNISSEUR(Roles.FOURNISSEUR);

  private final String code;

  Role(String code) {
    this.code = code;
  }

  /**
   * @return code du rôle realm Keycloak (identique au nom de la constante)
   */
  public String code() {
    return code;
  }

  /**
   * @param code code de rôle realm
   * @return le rôle CSE correspondant, vide si le code n'est pas un rôle CSE (ex. {@code
   *     default-roles-cse})
   */
  public static Optional<Role> depuisCode(String code) {
    return Arrays.stream(values()).filter(r -> r.code.equals(code)).findFirst();
  }

  /**
   * Détermine le rôle principal parmi des codes de rôles realm.
   *
   * @param codes codes de rôles realm (les rôles non CSE sont ignorés)
   * @return le rôle CSE le plus prioritaire, vide si aucun
   */
  public static Optional<Role> principal(Collection<String> codes) {
    return Arrays.stream(values()).filter(r -> codes.contains(r.code)).findFirst();
  }
}
