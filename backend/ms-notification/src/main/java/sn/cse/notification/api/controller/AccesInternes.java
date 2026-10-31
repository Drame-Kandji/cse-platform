package sn.cse.notification.api.controller;

import sn.cse.common.security.Roles;

/** Expression d'autorisation partagée : tous les rôles du personnel CSE. */
final class AccesInternes {

  static final String EXPRESSION =
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

  private AccesInternes() {}
}
