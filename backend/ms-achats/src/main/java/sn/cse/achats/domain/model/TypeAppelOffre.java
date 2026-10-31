package sn.cse.achats.domain.model;

/** Procédure de mise en concurrence. */
public enum TypeAppelOffre {
  /** Appel d'offres ouvert à tout fournisseur actif de la catégorie. */
  APPEL_OFFRES,
  /** Demande de prix restreinte aux fournisseurs invités. */
  DEMANDE_PRIX
}
