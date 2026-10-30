package sn.cse.suivi.api.controller;

import sn.cse.common.security.Roles;

/**
 * Expressions {@code @PreAuthorize} du service, construites depuis {@link Roles}. La Direction
 * Générale est en lecture seule (R12), à l'exception de la décision de réforme (R13).
 */
public final class Acces {

  /** Tous les rôles internes (lecture). */
  public static final String INTERNES =
      "hasAnyRole('"
          + Roles.CHEF_CHANTIER
          + "','"
          + Roles.RESPONSABLE_MATERIEL
          + "','"
          + Roles.RESPONSABLE_ACHATS
          + "','"
          + Roles.DAF
          + "','"
          + Roles.DIRECTION_GENERALE
          + "','"
          + Roles.ADMIN_SYSTEME
          + "')";

  /** Responsable Matériel uniquement. */
  public static final String RM = "hasRole('" + Roles.RESPONSABLE_MATERIEL + "')";

  /** Responsable Matériel ou administrateur. */
  public static final String RM_OU_ADMIN =
      "hasAnyRole('" + Roles.RESPONSABLE_MATERIEL + "','" + Roles.ADMIN_SYSTEME + "')";

  /** Chef de chantier (sur ses chantiers, contrôlé par le cas d'utilisation) ou RM. */
  public static final String CHEF_OU_RM =
      "hasAnyRole('" + Roles.CHEF_CHANTIER + "','" + Roles.RESPONSABLE_MATERIEL + "')";

  /** Direction Générale uniquement. */
  public static final String DG = "hasRole('" + Roles.DIRECTION_GENERALE + "')";

  /** Administrateur système uniquement. */
  public static final String ADMIN = "hasRole('" + Roles.ADMIN_SYSTEME + "')";

  /** Type de média des erreurs RFC 7807. */
  public static final String PROBLEM_JSON = "application/problem+json";

  private Acces() {}
}
