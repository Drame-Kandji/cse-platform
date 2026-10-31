import { http } from '@/shared/api/http'
import type { PageResponse, QueryParams } from '@/shared/api/types'

interface StatistiquesBesoins {
  readonly total: number
  readonly parStatut: Readonly<Record<string, number | undefined>>
}

/** Nombre total d'elements d'une liste paginee (une seule ligne demandee). */
async function countOf(path: string, query: QueryParams, signal?: AbortSignal): Promise<number> {
  const page = await http.get<PageResponse<unknown>>(path, { query: { ...query, page: 0, size: 1 }, signal })
  return page.totalElements
}

/** Compteurs des files de travail ("A traiter"), lus sur les endpoints metier existants. */
export const queuesApi = {
  besoinsEnAttente: async (signal?: AbortSignal) => {
    const stats = await http.get<StatistiquesBesoins>('/besoins/statistiques', { signal })
    return stats.parStatut.EN_ATTENTE ?? 0
  },
  besoinsCritiquesEnAttente: (signal?: AbortSignal) =>
    countOf('/besoins', { statut: 'EN_ATTENTE', priorite: 'CRITIQUE' }, signal),
  pannesSignalees: (signal?: AbortSignal) => countOf('/pannes', { statut: 'SIGNALEE' }, signal),
  aoPublies: (signal?: AbortSignal) => countOf('/appels-offres', { statut: 'PUBLIE' }, signal),
  aoClotures: (signal?: AbortSignal) => countOf('/appels-offres', { statut: 'CLOTURE' }, signal),
  besoinsAAcheter: (signal?: AbortSignal) => countOf('/besoins-a-acheter', {}, signal),
  engagementsEnAttente: (signal?: AbortSignal) => countOf('/engagements', { statut: 'EN_ATTENTE' }, signal),
  facturesEnRetard: (signal?: AbortSignal) => countOf('/factures', { enRetard: true }, signal),
  receptionsAConfirmer: (signal?: AbortSignal) =>
    countOf('/affectations', { statut: 'EN_ATTENTE_RECEPTION' }, signal),
}

export type QueueSource = keyof typeof queuesApi
