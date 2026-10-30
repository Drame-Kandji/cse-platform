package sn.cse.common.security;

import java.util.List;

/** Rôles realm Keycloak de la plateforme CSE (source unique, voir docs/API_CONTRACT.md §0). */
public final class Roles {

  public static final String CHEF_CHANTIER = "CHEF_CHANTIER";
  public static final String RESPONSABLE_MATERIEL = "RESPONSABLE_MATERIEL";
  public static final String RESPONSABLE_ACHATS = "RESPONSABLE_ACHATS";
  public static final String DAF = "DAF";
  public static final String DIRECTION_GENERALE = "DIRECTION_GENERALE";
  public static final String ADMIN_SYSTEME = "ADMIN_SYSTEME";
  public static final String FOURNISSEUR = "FOURNISSEUR";

  /** Rôles du personnel CSE (application interne). */
  public static final List<String> INTERNES =
      List.of(
          CHEF_CHANTIER,
          RESPONSABLE_MATERIEL,
          RESPONSABLE_ACHATS,
          DAF,
          DIRECTION_GENERALE,
          ADMIN_SYSTEME);

  /** Préfixe Spring Security des autorités issues des rôles. */
  public static final String PREFIX = "ROLE_";

  private Roles() {}
}
