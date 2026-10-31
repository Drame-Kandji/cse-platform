package sn.cse.achats.domain.model;

/** Priorité d'un besoin transmis aux achats. */
public enum Priorite {
  /** Priorité normale. */
  NORMAL,
  /** Besoin urgent. */
  URGENT,
  /** Besoin critique (arrêt de chantier). */
  CRITIQUE
}
