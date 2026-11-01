package sn.cse.achats.domain.exception;

/**
 * Codes d'erreur stables exposés au client dans le champ {@code code} des ProblemDetail. Ils
 * permettent au frontend d'afficher un message adapté sans analyser le texte.
 */
public final class CodesErreur {

  /** R02 : dépôt d'offre après la date limite. */
  public static final String DATE_LIMITE_DEPASSEE = "DATE_LIMITE_DEPASSEE";

  /** La date limite d'un appel d'offres doit être dans le futur. */
  public static final String DATE_LIMITE_INVALIDE = "DATE_LIMITE_INVALIDE";

  /** La somme des poids des critères doit valoir 100. */
  public static final String PONDERATION_INVALIDE = "PONDERATION_INVALIDE";

  /** Une demande de prix exige au moins un fournisseur invité. */
  public static final String INVITES_OBLIGATOIRES = "INVITES_OBLIGATOIRES";

  /** R14 : fournisseur suspendu, blacklisté ou non validé. */
  public static final String FOURNISSEUR_NON_ELIGIBLE = "FOURNISSEUR_NON_ELIGIBLE";

  /** Fournisseur inconnu du référentiel (ni ms-fournisseurs, ni projection locale). */
  public static final String FOURNISSEUR_INCONNU = "FOURNISSEUR_INCONNU";

  /** Demande de prix : seul un fournisseur invité peut soumissionner. */
  public static final String FOURNISSEUR_NON_INVITE = "FOURNISSEUR_NON_INVITE";

  /** Un fournisseur ne dépose qu'une offre par appel d'offres. */
  public static final String OFFRE_DEJA_DEPOSEE = "OFFRE_DEJA_DEPOSEE";

  /** Opération réservée à un appel d'offres publié. */
  public static final String AO_NON_PUBLIE = "AO_NON_PUBLIE";

  /** Opération réservée à un appel d'offres clôturé. */
  public static final String AO_NON_CLOTURE = "AO_NON_CLOTURE";

  /** L'appel d'offres est déjà attribué. */
  public static final String AO_DEJA_ATTRIBUE = "AO_DEJA_ATTRIBUE";

  /** R03 : un contrat ne naît que d'un appel d'offres attribué. */
  public static final String AO_NON_ATTRIBUE = "AO_NON_ATTRIBUE";

  /** Les critères PRIX et DELAI sont calculés, jamais saisis. */
  public static final String CRITERE_CALCULE = "CRITERE_CALCULE_AUTOMATIQUEMENT";

  /** Critère absent de la pondération de l'appel d'offres. */
  public static final String CRITERE_NON_RETENU = "CRITERE_NON_RETENU";

  /** Note hors de l'intervalle 0..100. */
  public static final String NOTE_INVALIDE = "NOTE_INVALIDE";

  /** R11 : une proposition d'attribution est déjà en cours d'approbation. */
  public static final String ATTRIBUTION_EN_COURS = "ATTRIBUTION_EN_COURS";

  /** Aucune proposition d'attribution en cours. */
  public static final String AUCUNE_ATTRIBUTION = "AUCUNE_ATTRIBUTION";

  /** R11 : le montant ne dépasse pas le seuil, l'approbation DG n'est pas requise. */
  public static final String APPROBATION_DG_NON_REQUISE = "APPROBATION_DG_NON_REQUISE";

  /** La décision a déjà été prise. */
  public static final String DECISION_DEJA_PRISE = "DECISION_DEJA_PRISE";

  /** Justification obligatoire. */
  public static final String JUSTIFICATION_OBLIGATOIRE = "JUSTIFICATION_OBLIGATOIRE";

  /** Procès-verbal non disponible au stade actuel de l'appel d'offres. */
  public static final String PV_INDISPONIBLE = "PV_INDISPONIBLE";

  /** Transition d'état interdite. */
  public static final String TRANSITION_INTERDITE = "TRANSITION_INTERDITE";

  /** Un contrat existe déjà pour cet appel d'offres. */
  public static final String CONTRAT_EXISTANT = "CONTRAT_EXISTANT";

  /** Les clauses ne sont modifiables qu'au statut BROUILLON. */
  public static final String CONTRAT_NON_MODIFIABLE = "CONTRAT_NON_MODIFIABLE";

  /** R06 : livraison impossible tant que la DAF n'a pas validé l'engagement. */
  public static final String ENGAGEMENT_NON_VALIDE = "ENGAGEMENT_NON_VALIDE";

  /** Aucun engagement en attente de décision. */
  public static final String ENGAGEMENT_NON_EN_ATTENTE = "ENGAGEMENT_NON_EN_ATTENTE";

  /** Opération réservée à un contrat en cours d'exécution. */
  public static final String CONTRAT_NON_EN_EXECUTION = "CONTRAT_NON_EN_EXECUTION";

  /** Le fournisseur ne renseigne ses jalons qu'une fois l'expédition déclarée. */
  public static final String CONTRAT_NON_EN_LIVRAISON = "CONTRAT_NON_EN_LIVRAISON";

  /** Portail : une facture n'est déposée que sur un contrat livré (ou clôturé). */
  public static final String CONTRAT_NON_LIVRE = "CONTRAT_NON_LIVRE";

  /** Date d'expédition antérieure à la signature du contrat. */
  public static final String DATE_EXPEDITION_INVALIDE = "DATE_EXPEDITION_INVALIDE";

  /** Date postérieure à aujourd'hui. */
  public static final String DATE_FUTURE = "DATE_FUTURE";

  /** Date de livraison antérieure à la signature. */
  public static final String DATE_LIVRAISON_INVALIDE = "DATE_LIVRAISON_INVALIDE";

  /** Des factures restent à payer : clôture impossible. */
  public static final String FACTURES_EN_COURS = "FACTURES_EN_COURS";

  /** Le contrat n'est pas (ou plus) facturable. */
  public static final String CONTRAT_NON_FACTURABLE = "CONTRAT_NON_FACTURABLE";

  /** Le cumul facturé dépasserait le montant du contrat. */
  public static final String DEPASSEMENT_MONTANT_CONTRAT = "DEPASSEMENT_MONTANT_CONTRAT";

  /** L'échéance précède l'émission. */
  public static final String ECHEANCE_INVALIDE = "ECHEANCE_INVALIDE";

  /** Numéro de facture déjà enregistré pour ce fournisseur. */
  public static final String FACTURE_EXISTANTE = "FACTURE_EXISTANTE";

  /** Motif obligatoire. */
  public static final String MOTIF_OBLIGATOIRE = "MOTIF_OBLIGATOIRE";

  /** Besoin déjà rattaché à un appel d'offres. */
  public static final String BESOIN_DEJA_EN_ACHAT = "BESOIN_DEJA_EN_ACHAT";

  /** Besoin non (encore) transmis aux achats. */
  public static final String BESOIN_NON_EN_ACHAT = "BESOIN_NON_EN_ACHAT";

  /** Compte portail sans fournisseur rattaché. */
  public static final String COMPTE_NON_RATTACHE = "COMPTE_NON_RATTACHE";

  /** Donnée métier invalide. */
  public static final String DONNEE_INVALIDE = "DONNEE_INVALIDE";

  private CodesErreur() {}
}
