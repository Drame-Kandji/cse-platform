package sn.cse.achats.domain.model;

/** Statut d'un fournisseur tel que connu de ms-achats (projection de ms-fournisseurs). */
public enum StatutFournisseur {
  /** Inscription en attente de validation par le Responsable des Achats. */
  EN_ATTENTE_VALIDATION,
  /** Fournisseur actif. */
  ACTIF,
  /** Fournisseur suspendu. */
  SUSPENDU,
  /** Fournisseur blacklisté (définitif). */
  BLACKLISTE;

  /**
   * Règle R14 : seul un fournisseur actif peut être invité ou soumissionner ; un fournisseur
   * suspendu, blacklisté ou non encore validé est exclu.
   *
   * @return vrai si le fournisseur peut participer à une consultation
   */
  public boolean peutParticiper() {
    return this == ACTIF;
  }
}
