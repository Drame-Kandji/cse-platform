package sn.cse.achats.api.controller;

import sn.cse.common.security.Roles;

/** Expressions {@code @PreAuthorize} de ms-achats, construites sur les constantes {@link Roles}. */
final class Acces {

  private static final String RA = "'" + Roles.RESPONSABLE_ACHATS + "'";
  private static final String RM = "'" + Roles.RESPONSABLE_MATERIEL + "'";
  private static final String DAF_ = "'" + Roles.DAF + "'";
  private static final String DG_ = "'" + Roles.DIRECTION_GENERALE + "'";

  static final String ACHATS = "hasRole(" + RA + ")";
  static final String MATERIEL = "hasRole(" + RM + ")";
  static final String DG = "hasRole(" + DG_ + ")";
  static final String DAF = "hasRole(" + DAF_ + ")";
  static final String FOURNISSEUR = "hasRole('" + Roles.FOURNISSEUR + "')";
  static final String ACHATS_MATERIEL = "hasAnyRole(" + RA + "," + RM + ")";
  static final String LECTURE_AO = "hasAnyRole(" + RA + "," + RM + "," + DG_ + "," + DAF_ + ")";
  static final String LECTURE_OFFRES = "hasAnyRole(" + RA + "," + RM + "," + DG_ + ")";
  static final String LECTURE_CONTRATS =
      "hasAnyRole(" + RA + "," + RM + "," + DAF_ + "," + DG_ + ")";
  static final String LECTURE_ENGAGEMENTS = "hasAnyRole(" + DAF_ + "," + RA + "," + DG_ + ")";
  static final String FACTURES = "hasAnyRole(" + DAF_ + "," + RA + ")";
  static final String FINANCE = "hasAnyRole(" + DAF_ + "," + DG_ + ")";

  private Acces() {}
}
