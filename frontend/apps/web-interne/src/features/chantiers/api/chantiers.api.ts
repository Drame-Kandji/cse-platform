import { http, type PageParams, type PageResponse } from '@/shared/api'
import type { Affectation, Chantier } from '@/shared/domain/types'
import type { ChantierFiltres, ChantierModification, ChantierPayload, ChefChantierOption } from './types'

/** Appels HTTP chantiers (ms-suivi) et lectures annexes de l'ecran. */
export const chantiersApi = {
  lister: (params: ChantierFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Chantier>>('/chantiers', { query: { ...params }, signal }),
  detail: (id: string, signal?: AbortSignal) => http.get<Chantier>(`/chantiers/${id}`, { signal }),
  creer: (body: ChantierPayload) => http.post<Chantier>('/chantiers', body),
  modifier: (id: string, body: ChantierModification) => http.put<Chantier>(`/chantiers/${id}`, body),
  terminer: (id: string) => http.patch<Chantier>(`/chantiers/${id}/terminer`),
  /** Affectations en cours d'un chantier (vue detail). */
  affectations: (chantierId: string, signal?: AbortSignal) =>
    http.get<PageResponse<Affectation>>('/affectations', {
      query: { chantierId, statut: 'EN_COURS', size: 20 },
      signal,
    }),
  /** Comptes chefs de chantier actifs (ADMIN uniquement). */
  chefs: (signal?: AbortSignal) =>
    http.get<PageResponse<ChefChantierOption>>('/admin/utilisateurs', {
      query: { role: 'CHEF_CHANTIER', actif: true, size: 50 },
      signal,
    }),
}
