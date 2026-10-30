package sn.cse.suivi.domain.model;

/** Nature d'une intervention de maintenance. */
public enum TypeMaintenance {
  /** Entretien planifié (vidange, filtres…). */
  PREVENTIVE,
  /** Réparation suite à panne. */
  CORRECTIVE,
  /** Révision lourde (moteur, transmission…). */
  LOURDE
}
