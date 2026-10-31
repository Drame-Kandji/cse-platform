package sn.cse.chatbot.domain.model;

/** Intention détectée dans une question (classification déterministe). */
public enum Intention {
  /** Où se trouve un engin / sur quel chantier. */
  LOCALISER_ENGIN,
  /** Matériel disponible (par catégorie, désignation, quantité). */
  DISPONIBILITE,
  /** Avancement d'une expression de besoin. */
  STATUT_BESOIN,
  /** Avancement d'un contrat, d'un appel d'offres ou d'une livraison. */
  STATUT_COMMANDE,
  /** Maintenances et pannes d'un matériel. */
  HISTORIQUE_MAINTENANCE,
  /** Planning des affectations (chantier, matériel, période). */
  PLANNING_AFFECTATION,
  /** Fournisseurs classés pour une catégorie. */
  FOURNISSEURS_PAR_CATEGORIE,
  /** Question de procédure (« comment… »). */
  PROCEDURE,
  /** Hors périmètre ou non reconnue. */
  AUTRE
}
