package sn.cse.fournisseurs.api.controller;

import sn.cse.common.security.Roles;

/** Expressions {@code @PreAuthorize} du service (matrice d'accès du contrat §4). */
final class Acces {

  private static final String RA = "'" + Roles.RESPONSABLE_ACHATS + "'";
  private static final String RM = "'" + Roles.RESPONSABLE_MATERIEL + "'";
  private static final String DAF = "'" + Roles.DAF + "'";
  private static final String DG = "'" + Roles.DIRECTION_GENERALE + "'";

  /** Lecteurs du référentiel : RA, RM, DAF, DG. */
  static final String LECTEURS = "hasAnyRole(" + RA + "," + RM + "," + DAF + "," + DG + ")";

  /** Responsable des Achats uniquement. */
  static final String RA_SEUL = "hasRole(" + RA + ")";

  /** Direction Générale uniquement (R09, étape 2). */
  static final String DG_SEULE = "hasRole(" + DG + ")";

  /** Évaluateurs et ouverture de litiges : RA, RM. */
  static final String RA_RM = "hasAnyRole(" + RA + "," + RM + ")";

  /** Lecteurs des litiges : RA, RM, DG. */
  static final String LECTEURS_LITIGES = "hasAnyRole(" + RA + "," + RM + "," + DG + ")";

  /** Comptes du portail fournisseur. */
  static final String FOURNISSEUR = "hasRole('" + Roles.FOURNISSEUR + "')";

  /**
   * Route publique (inscription du portail) : doit aussi figurer dans {@code
   * cse.security.public-paths}, sinon la chaîne de filtres exige un jeton avant d'atteindre la
   * méthode.
   */
  static final String PUBLIC = "permitAll()";

  private Acces() {}
}
