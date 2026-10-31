import { env } from '@/shared/config/env'
import { ApiError, apiErrorFromResponse } from './errors'
import type { QueryParams } from './types'

/** Fournit le jeton d'acces courant (rafraichi si proche de l'expiration). */
export type TokenProvider = () => Promise<string | null>

interface HttpConfig {
  baseUrl: string
  getToken: TokenProvider
  onUnauthorized: () => void
}

const config: HttpConfig = {
  baseUrl: env.apiBaseUrl,
  getToken: () => Promise.resolve(null),
  onUnauthorized: () => undefined,
}

/**
 * Configure la couche HTTP (appele par AuthProvider, ou par les tests).
 * @param overrides champs a remplacer (jeton, callback 401, URL de base)
 */
export function configureHttp(overrides: Partial<HttpConfig>): void {
  Object.assign(config, overrides)
}

export type ResponseType = 'json' | 'blob' | 'text' | 'void'

export interface RequestOptions {
  readonly method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  readonly query?: QueryParams
  /** Corps JSON (objet) ou FormData (upload). */
  readonly body?: unknown
  readonly headers?: Readonly<Record<string, string>>
  readonly signal?: AbortSignal
  readonly responseType?: ResponseType
}

/** Genere un identifiant de correlation (UUID v4). */
export function createCorrelationId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
  return `${Date.now().toString(16)}-${Math.random().toString(16).slice(2)}`
}

/** Construit l'URL absolue d'un endpoint avec sa query string (valeurs vides ignorees). */
export function buildUrl(path: string, query?: QueryParams): string {
  const base = /^https?:\/\//.test(path) ? path : `${config.baseUrl}${path.startsWith('/') ? '' : '/'}${path}`
  const url = new URL(base, window.location.origin)
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value === undefined || value === null || value === '') continue
    if (Array.isArray(value)) {
      for (const item of value) url.searchParams.append(key, String(item))
    } else {
      url.searchParams.set(key, String(value))
    }
  }
  return url.toString()
}

async function buildHeaders(options: RequestOptions, correlationId: string): Promise<Headers> {
  const headers = new Headers(options.headers)
  headers.set('X-Correlation-Id', correlationId)
  if (!headers.has('Accept')) {
    headers.set(
      'Accept',
      options.responseType === 'blob' ? '*/*' : 'application/json, application/problem+json',
    )
  }
  if (options.body !== undefined && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  const token = await config.getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  return headers
}

/**
 * Envoie une requete et retourne la Response brute (en-tetes accessibles).
 * Utiliser `request` dans la grande majorite des cas.
 * @throws ApiError pour toute reponse non 2xx (ProblemDetail analyse) ou erreur reseau (status 0)
 */
export async function send(path: string, options: RequestOptions = {}): Promise<Response> {
  const correlationId = createCorrelationId()
  const headers = await buildHeaders(options, correlationId)
  const body =
    options.body === undefined
      ? undefined
      : options.body instanceof FormData
        ? options.body
        : JSON.stringify(options.body)

  let response: Response
  try {
    response = await fetch(buildUrl(path, options.query), {
      method: options.method ?? 'GET',
      headers,
      body,
      signal: options.signal,
      credentials: 'same-origin',
    })
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') throw cause
    throw new ApiError(0, { code: 'NETWORK_ERROR' }, correlationId)
  }

  if (!response.ok) {
    const error = await apiErrorFromResponse(response, correlationId)
    if (error.status === 401) config.onUnauthorized()
    throw error
  }
  return response
}

/**
 * Execute une requete HTTP vers la passerelle et retourne le corps type.
 * @throws ApiError pour toute reponse non 2xx ou erreur reseau
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await send(path, options)
  return parseBody<T>(response, options.responseType ?? 'json')
}

async function parseBody<T>(response: Response, type: ResponseType): Promise<T> {
  if (type === 'void' || response.status === 204) return undefined as T
  if (type === 'blob') return (await response.blob()) as T
  if (type === 'text') return (await response.text()) as T
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

type BodylessOptions = Omit<RequestOptions, 'method' | 'body'>
type BodyOptions = Omit<RequestOptions, 'method'>

/** Raccourcis HTTP types. Exemple : `http.get<Materiel>('/materiels/42')`. */
export const http = {
  get: <T>(path: string, options?: BodylessOptions) => request<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: BodyOptions) =>
    request<T>(path, { ...options, body, method: 'POST' }),
  put: <T>(path: string, body?: unknown, options?: BodyOptions) =>
    request<T>(path, { ...options, body, method: 'PUT' }),
  patch: <T>(path: string, body?: unknown, options?: BodyOptions) =>
    request<T>(path, { ...options, body, method: 'PATCH' }),
  del: <T = void>(path: string, options?: BodylessOptions) =>
    request<T>(path, { ...options, method: 'DELETE', responseType: options?.responseType ?? 'void' }),
}
