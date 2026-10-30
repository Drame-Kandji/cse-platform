package sn.cse.besoins.domain.model;

/** Événements de domaine émis par l'agrégat {@link BesoinMateriel}. */
public enum TypeEvenementBesoin {
  SOUMIS,
  MODIFIE,
  VALIDE,
  REJETE,
  AFFECTE_DIRECT,
  EN_ACHAT
}
