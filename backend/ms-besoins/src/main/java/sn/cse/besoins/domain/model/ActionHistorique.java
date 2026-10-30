package sn.cse.besoins.domain.model;

/** Nature d'une entrée de l'historique d'un besoin. */
public enum ActionHistorique {
  /** Création du besoin par le demandeur. */
  SOUMISSION,
  /** Modification du contenu tant que le besoin est en attente. */
  MODIFICATION,
  /** Approbation d'une étape intermédiaire de la chaîne de validation. */
  APPROBATION_ETAPE,
  /** Validation finale. */
  VALIDATION,
  /** Rejet motivé. */
  REJET,
  /** Orientation impossible (service de suivi indisponible), à relancer. */
  ORIENTATION_EN_ATTENTE,
  /** Affectation directe de matériels du parc. */
  AFFECTATION_DIRECTE,
  /** Transmission au service Achats. */
  ORIENTATION_ACHAT,
  /** Affectation d'un matériel notifiée par ms-suivi (événement MATERIEL_AFFECTE). */
  MATERIEL_AFFECTE
}
