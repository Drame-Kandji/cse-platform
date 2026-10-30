package sn.cse.suivi.domain.model;

/** Nature d'un mouvement tracé dans l'historique d'un matériel. */
public enum TypeMouvement {
  /** Affectation à un chantier. */
  AFFECTATION,
  /** Réception confirmée sur chantier. */
  RECEPTION,
  /** Retour au parc. */
  LIBERATION,
  /** Entrée ou sortie de maintenance. */
  MAINTENANCE,
  /** Demande, refus ou validation de réforme. */
  REFORME,
  /** Entrée dans le parc. */
  ENREGISTREMENT
}
