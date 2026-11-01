import { http, HttpResponse, type JsonBodyType } from 'msw'
import { setupServer } from 'msw/node'
import type { PageResponse } from '@/shared/api/types'

/** URL absolue d'un endpoint du portail (base `/api` resolue sur l'origine jsdom). */
export const api = (path: string) => `${window.location.origin}/api/portail${path}`

/** Page de resultats pour les reponses simulees. */
export function page<T>(content: T[]): PageResponse<T> {
  return { content, page: 0, size: 50, totalElements: content.length, totalPages: 1, first: true, last: true }
}

/** ProblemDetail RFC 7807 simule. */
export function problem(status: number, code: string, detail = 'Erreur') {
  return HttpResponse.json({ status, code, title: 'Erreur', detail } satisfies JsonBodyType, {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

/** Serveur MSW partage : chaque test ajoute ses handlers avec `server.use(...)`. */
export const server = setupServer()
export { http, HttpResponse }
