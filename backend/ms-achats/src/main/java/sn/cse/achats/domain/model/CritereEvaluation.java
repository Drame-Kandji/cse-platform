package sn.cse.achats.domain.model;

/** Critère d'évaluation des offres. */
public enum CritereEvaluation {
  /** Prix proposé : note calculée relativement à la meilleure offre. */
  PRIX(true),
  /** Délai de livraison : note calculée relativement à la meilleure offre. */
  DELAI(true),
  /** Qualité technique : note saisie (0..100). */
  QUALITE_TECHNIQUE(false),
  /** Garantie : note saisie (0..100). */
  GARANTIE(false),
  /** Service après-vente : note saisie (0..100). */
  SAV(false),
  /** Références du fournisseur : note saisie (0..100). */
  REFERENCES(false);

  private final boolean calcule;

  CritereEvaluation(boolean calcule) {
    this.calcule = calcule;
  }

  /**
   * @return vrai si la note est calculée automatiquement (jamais saisie)
   */
  public boolean estCalcule() {
    return calcule;
  }
}
