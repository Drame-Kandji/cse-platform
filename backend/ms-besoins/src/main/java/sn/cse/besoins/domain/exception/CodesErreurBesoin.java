package sn.cse.besoins.domain.exception;

/** Codes d'erreur stables exposés au client (champ {@code code} des ProblemDetail). */
public final class CodesErreurBesoin {

  /** R05 : besoin validé ou au-delà, plus modifiable. */
  public static final String BESOIN_NON_MODIFIABLE = "BESOIN_NON_MODIFIABLE";

  /** R08 : justification exigée pour une priorité URGENT ou CRITIQUE. */
  public static final String JUSTIFICATION_OBLIGATOIRE = "JUSTIFICATION_OBLIGATOIRE";

  /** R08 : motif exigé pour un rejet. */
  public static final String MOTIF_REJET_OBLIGATOIRE = "MOTIF_REJET_OBLIGATOIRE";

  /** R01 : l'acteur ne détient pas le rôle exigé par l'étape de validation. */
  public static final String VALIDATION_NON_AUTORISEE = "VALIDATION_NON_AUTORISEE";

  /** Transition refusée par la machine à états. */
  public static final String TRANSITION_STATUT_INTERDITE = "TRANSITION_STATUT_INTERDITE";

  /** Relance d'orientation demandée sur un besoin qui n'est pas en attente d'orientation. */
  public static final String ORIENTATION_NON_REQUISE = "ORIENTATION_NON_REQUISE";

  /** Version fournie différente de la version courante (verrou optimiste). */
  public static final String CONFLIT_CONCURRENCE = "CONFLIT_CONCURRENCE";

  /** Seul le demandeur peut modifier son besoin. */
  public static final String MODIFICATION_RESERVEE_DEMANDEUR = "MODIFICATION_RESERVEE_DEMANDEUR";

  /** Données incohérentes (quantité, durée, date…). */
  public static final String BESOIN_INVALIDE = "BESOIN_INVALIDE";

  /** Date de besoin antérieure à la date de soumission. */
  public static final String DATE_BESOIN_PASSEE = "DATE_BESOIN_PASSEE";

  private CodesErreurBesoin() {}
}
