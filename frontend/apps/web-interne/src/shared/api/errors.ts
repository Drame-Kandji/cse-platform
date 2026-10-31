import { z } from 'zod'
import type { FieldError, ProblemDetail } from './types'

const problemSchema = z
  .object({
    type: z.string().optional(),
    title: z.string().optional(),
    status: z.number().int().optional(),
    detail: z.string().optional(),
    instance: z.string().optional(),
    code: z.string().optional(),
    traceId: z.string().optional(),
    errors: z.array(z.object({ field: z.string(), message: z.string() })).optional(),
  })
  .passthrough()

/** Messages francais par defaut selon le statut HTTP. */
const STATUS_MESSAGES: Readonly<Record<number, string>> = {
  0: 'Impossible de joindre le serveur. Vérifiez votre connexion.',
  400: 'La requête est invalide.',
  401: 'Votre session a expiré. Veuillez vous reconnecter.',
  403: "Vous n'avez pas les droits nécessaires pour cette action.",
  404: 'La ressource demandée est introuvable.',
  409: 'Conflit : la ressource a été modifiée entre-temps. Rechargez puis réessayez.',
  413: 'Le fichier envoyé est trop volumineux.',
  422: 'Certaines données sont invalides.',
  429: 'Trop de requêtes. Patientez quelques instants.',
  500: 'Une erreur interne est survenue.',
  502: 'Le service est momentanément indisponible.',
  503: 'Le service est momentanément indisponible.',
  504: 'Le serveur met trop de temps à répondre.',
}

/** Message par defaut pour un statut HTTP donne. */
export function defaultMessageForStatus(status: number): string {
  return STATUS_MESSAGES[status] ?? (status >= 500 ? STATUS_MESSAGES[500] : STATUS_MESSAGES[400]) ?? ''
}

/**
 * Erreur API normalisee, construite a partir d'un ProblemDetail RFC 7807.
 * Toute erreur HTTP (ou reseau, status 0) levee par `shared/api` est une ApiError.
 */
export class ApiError extends Error {
  override readonly name = 'ApiError'
  readonly status: number
  readonly code: string | undefined
  readonly title: string
  readonly detail: string | undefined
  readonly type: string | undefined
  readonly traceId: string | undefined
  readonly correlationId: string | undefined
  readonly fieldErrors: readonly FieldError[]

  constructor(status: number, problem: ProblemDetail = {}, correlationId?: string) {
    const title = problem.title ?? defaultMessageForStatus(status)
    super(problem.detail ?? title)
    this.status = status
    this.code = problem.code
    this.title = title
    this.detail = problem.detail
    this.type = problem.type
    this.traceId = problem.traceId
    this.correlationId = correlationId
    this.fieldErrors = problem.errors ?? []
  }

  /** Erreur reseau (serveur injoignable, CORS, coupure). */
  get isNetworkError(): boolean {
    return this.status === 0
  }

  /** Erreur client 4xx : inutile de reessayer automatiquement. */
  get isClientError(): boolean {
    return this.status >= 400 && this.status < 500
  }

  /** Message francais a afficher a l'utilisateur. */
  get userMessage(): string {
    return this.detail ?? this.title
  }
}

/** Indique si une valeur est une ApiError. */
export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

/**
 * Analyse un corps d'erreur JSON en ProblemDetail ; ignore les champs inconnus
 * et retourne un objet vide si le corps n'est pas conforme.
 */
export function parseProblemDetail(body: unknown): ProblemDetail {
  const result = problemSchema.safeParse(body)
  return result.success ? result.data : {}
}

/** Construit une ApiError a partir d'une Response en echec. */
export async function apiErrorFromResponse(response: Response, correlationId?: string): Promise<ApiError> {
  const contentType = response.headers.get('content-type') ?? ''
  let problem: ProblemDetail = {}
  if (contentType.includes('json')) {
    try {
      problem = parseProblemDetail(await response.json())
    } catch {
      problem = {}
    }
  }
  return new ApiError(
    response.status,
    problem,
    correlationId ?? response.headers.get('x-correlation-id') ?? undefined,
  )
}

/**
 * Message francais pour n'importe quelle erreur (toasts, etats d'erreur).
 * @example toast.error(getErrorMessage(error))
 */
export function getErrorMessage(error: unknown): string {
  if (isApiError(error)) return error.userMessage
  if (error instanceof Error && error.message) return error.message
  return 'Une erreur inattendue est survenue.'
}
