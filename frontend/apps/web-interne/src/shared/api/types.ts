/** Page de resultats renvoyee par tous les endpoints pagines (PageResponse<T> cote Java). */
export interface PageResponse<T> {
  readonly content: readonly T[]
  /** Index de page, base 0. */
  readonly page: number
  readonly size: number
  readonly totalElements: number
  readonly totalPages: number
  readonly first: boolean
  readonly last: boolean
}

/** Parametres de pagination standard (defaut backend : size 20, max 50). */
export interface PageParams {
  readonly page?: number
  readonly size?: number
  /** Tri Spring : "champ,asc" ou "champ,desc" (champs whitelistes cote backend). */
  readonly sort?: string
}

/** Erreur de champ d'un ProblemDetail RFC 7807. */
export interface FieldError {
  readonly field: string
  readonly message: string
}

/** Corps d'erreur RFC 7807 renvoye par les services CSE. */
export interface ProblemDetail {
  readonly type?: string
  readonly title?: string
  readonly status?: number
  readonly detail?: string
  readonly instance?: string
  readonly code?: string
  readonly traceId?: string
  readonly errors?: readonly FieldError[]
}

/** Valeur acceptee dans une query string (les tableaux sont repetes : ?a=1&a=2). */
export type QueryValue = string | number | boolean | null | undefined | readonly (string | number)[]
export type QueryParams = Readonly<Record<string, QueryValue>>

/** Taille de page par defaut alignee sur le backend. */
export const DEFAULT_PAGE_SIZE = 20
/** Tailles de page proposees (le backend plafonne a 50). */
export const PAGE_SIZE_OPTIONS = [10, 20, 50] as const
