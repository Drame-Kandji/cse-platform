package sn.cse.achats.domain.model;

/** Résultat d'une offre vu par son auteur (portail fournisseur). */
public enum ResultatOffre {
  /** Appel d'offres non encore attribué. */
  EN_COURS,
  /** Offre retenue : le marché est attribué à ce fournisseur. */
  RETENUE,
  /** Marché attribué à un autre soumissionnaire. */
  NON_RETENUE
}
