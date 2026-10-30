package sn.cse.fournisseurs.domain.model;

/** Nature d'une entrée de la chronologie d'un litige. */
public enum TypeActionLitige {
  /** Création du litige (automatique). */
  OUVERTURE,
  /** Action de suivi (relance, expertise, réunion…). */
  ACTION,
  /** Résolution du litige. */
  RESOLUTION,
  /** Classement sans suite. */
  CLASSEMENT
}
