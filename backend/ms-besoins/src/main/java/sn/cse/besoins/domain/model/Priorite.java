package sn.cse.besoins.domain.model;

/** Priorité d'un besoin, ordonnée de la moins à la plus urgente. */
public enum Priorite {
  /** Besoin planifié. */
  NORMAL,
  /** Besoin pressant : justification obligatoire (R08). */
  URGENT,
  /** Besoin bloquant pour le chantier : justification obligatoire (R08). */
  CRITIQUE;

  /**
   * Règle R08 : une priorité élevée doit être justifiée par le demandeur.
   *
   * @return vrai si une justification est exigée
   */
  public boolean exigeJustification() {
    return this != NORMAL;
  }

  /**
   * @param autre priorité comparée
   * @return la plus élevée des deux priorités
   */
  public Priorite max(Priorite autre) {
    return autre == null || compareTo(autre) >= 0 ? this : autre;
  }
}
