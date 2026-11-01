import { getErrorMessage, isApiError } from '@/shared/api/errors'

/**
 * Messages francais des codes metier renvoyes au portail (ProblemDetail `code`).
 * Certains codes existent sous deux noms (contrat d'API §13 et implementation ms-achats).
 */
export const MESSAGES_CODES: Readonly<Record<string, string>> = {
  DATE_LIMITE_DEPASSEE: 'La date limite de dépôt est dépassée : les offres ne sont plus acceptées.',
  FOURNISSEUR_NON_ELIGIBLE:
    "Votre entreprise n'est pas éligible : elle doit être active et couvrir la catégorie demandée.",
  FOURNISSEUR_NON_INVITE: 'Cette demande de prix est réservée aux fournisseurs invités.',
  OFFRE_DEJA_DEPOSEE: "Vous avez déjà déposé une offre pour cet appel d'offres.",
  TRANSITION_INVALIDE: "Cette action n'est plus possible : le contrat a changé de statut. Rechargez la page.",
  TRANSITION_INTERDITE:
    "Cette action n'est plus possible : le contrat a changé de statut. Rechargez la page.",
  ENGAGEMENT_NON_VALIDE: "L'engagement financier n'est pas encore validé par la CSE : expédition impossible.",
  CONTRAT_NON_LIVRE: "La facture ne peut être déposée qu'après la livraison du contrat.",
  CONTRAT_NON_FACTURABLE: "La facture ne peut être déposée qu'après la livraison du contrat.",
  FOURNISSEUR_EXISTANT: 'Une entreprise avec ce NINEA ou cette adresse e-mail est déjà inscrite.',
  FACTURE_EXISTANTE: 'Une facture avec ce numéro existe déjà.',
  DEPASSEMENT_MONTANT_CONTRAT: 'Le montant facturé dépasse le montant du contrat.',
  ECHEANCE_INVALIDE: "L'échéance doit suivre la date d'émission.",
  CONTRAT_NON_EN_LIVRAISON: "Les jalons se renseignent une fois l'expédition déclarée.",
  DATE_EXPEDITION_INVALIDE: "La date d'expédition ne peut pas précéder la signature du contrat.",
  DATE_FUTURE: 'La date ne peut pas être dans le futur.',
  MOTIF_OBLIGATOIRE: 'Le motif est obligatoire.',
  COMPTE_NON_RATTACHE: "Votre compte n'est rattaché à aucune entreprise. Contactez le service achats.",
  CONFLIT_CONCURRENCE: 'Ces informations ont été modifiées entre-temps. Rechargez la page puis réessayez.',
}

export interface MessageOptions {
  /** Message d'un 409 sans code connu (ex. offre deja deposee). */
  readonly conflit?: string
}

/**
 * Message francais d'une erreur du portail : code metier connu, sinon 409 contextualise,
 * sinon message du serveur (ProblemDetail `detail`) ou message par defaut du statut HTTP.
 * @example toast.error(messageErreur(error, { conflit: 'Offre déjà déposée' }))
 */
export function messageErreur(error: unknown, options: MessageOptions = {}): string {
  if (isApiError(error)) {
    const parCode = error.code ? MESSAGES_CODES[error.code] : undefined
    if (parCode) return parCode
    if (error.status === 409 && options.conflit) return options.conflit
  }
  return getErrorMessage(error)
}

/** Vrai si l'erreur est un 404 (ressource absente ou endpoint pas encore deploye). */
export function estIntrouvable(error: unknown): boolean {
  return isApiError(error) && error.status === 404
}
