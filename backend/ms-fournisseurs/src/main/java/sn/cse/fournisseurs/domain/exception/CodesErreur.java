package sn.cse.fournisseurs.domain.exception;

/** Codes d'erreur métier exposés dans le champ {@code code} des réponses ProblemDetail. */
public final class CodesErreur {

  /** Transition de statut non prévue par la machine à états (409). */
  public static final String TRANSITION_STATUT_INVALIDE = "TRANSITION_STATUT_INVALIDE";

  /** Une demande de blacklistage est déjà en attente de décision (409, R09). */
  public static final String DEMANDE_BLACKLISTAGE_EN_COURS = "DEMANDE_BLACKLISTAGE_EN_COURS";

  /** Aucune demande de blacklistage à arbitrer (409, R09). */
  public static final String AUCUNE_DEMANDE_BLACKLISTAGE = "AUCUNE_DEMANDE_BLACKLISTAGE";

  /** Le demandeur du blacklistage ne peut pas l'approuver lui-même (422, R09). */
  public static final String DECISION_PAR_DEMANDEUR_INTERDITE = "DECISION_PAR_DEMANDEUR_INTERDITE";

  /** Motif obligatoire manquant (422). */
  public static final String MOTIF_OBLIGATOIRE = "MOTIF_OBLIGATOIRE";

  /** Donnée de référentiel invalide (422). */
  public static final String DONNEE_INVALIDE = "DONNEE_INVALIDE";

  /** Note d'évaluation hors de l'intervalle autorisé (422). */
  public static final String NOTE_HORS_BORNES = "NOTE_HORS_BORNES";

  /** Le fournisseur ne peut pas être évalué dans son statut actuel (422). */
  public static final String FOURNISSEUR_NON_EVALUABLE = "FOURNISSEUR_NON_EVALUABLE";

  /** Un fournisseur blacklisté n'est plus modifiable (409). */
  public static final String FOURNISSEUR_BLACKLISTE = "FOURNISSEUR_BLACKLISTE";

  /** NINEA déjà utilisé par un autre fournisseur (409). */
  public static final String NINEA_DEJA_UTILISE = "NINEA_DEJA_UTILISE";

  /** Version obsolète (verrou optimiste, 409). */
  public static final String CONFLIT_CONCURRENCE = "CONFLIT_CONCURRENCE";

  /** Catégorie de catalogue hors des spécialités du fournisseur (422). */
  public static final String CATEGORIE_HORS_SPECIALITES = "CATEGORIE_HORS_SPECIALITES";

  /** Le litige est clos et n'accepte plus d'action (409). */
  public static final String LITIGE_CLOS = "LITIGE_CLOS";

  /** Type d'action de litige non autorisé par l'utilisateur (422). */
  public static final String ACTION_LITIGE_INVALIDE = "ACTION_LITIGE_INVALIDE";

  private CodesErreur() {}
}
