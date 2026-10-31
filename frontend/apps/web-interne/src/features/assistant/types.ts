/** Type d'objet metier cite par l'assistant (contrat §10). */
export type SourceType =
  'MATERIEL' | 'BESOIN' | 'CHANTIER' | 'CONTRAT' | 'FOURNISSEUR' | 'MAINTENANCE' | 'APPEL_OFFRE'

/** Objet metier sur lequel s'appuie une reponse. */
export interface Source {
  readonly type: SourceType
  readonly id: string
  readonly libelle: string
  /** Route front (ex. "/materiels/<id>"). */
  readonly lien?: string | null
}

/** Reponse de l'assistant a une question. */
export interface Reponse {
  readonly conversationId: string
  readonly messageId: string
  /** Markdown simple (paragraphes, gras, listes, code en ligne). */
  readonly reponse: string
  readonly sources: readonly Source[]
  readonly intention: string
  /** Vrai si le LLM est indisponible : reponse construite par modeles. */
  readonly modeDegrade: boolean
  readonly horodatage: string
}

export interface Question {
  /** 1000 caracteres maximum. */
  readonly question: string
  readonly conversationId?: string
}

export interface ConversationResume {
  readonly id: string
  readonly titre: string
  readonly dernierMessage: string
  readonly nombreMessages: number
  readonly updatedAt: string
}

export interface MessageConversation {
  readonly id: string
  readonly role: 'UTILISATEUR' | 'ASSISTANT'
  readonly contenu: string
  readonly sources: readonly Source[]
  readonly horodatage: string
}

export interface Conversation {
  readonly id: string
  readonly titre: string
  readonly messages: readonly MessageConversation[]
}

/** Message affiche dans le fil (etat local du panneau). */
export interface ChatMessage {
  readonly id: string
  readonly role: 'UTILISATEUR' | 'ASSISTANT'
  readonly contenu: string
  readonly sources: readonly Source[]
  readonly horodatage: string
  readonly modeDegrade?: boolean
  /** Message d'erreur affiche sous une question restee sans reponse. */
  readonly erreur?: string
}

/** Longueur maximale d'une question (contrat §10). */
export const QUESTION_MAX_LENGTH = 1000
