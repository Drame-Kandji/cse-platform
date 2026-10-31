import { http, type PageParams, type PageResponse } from '@/shared/api'
import type { Chantier, Disponibilite, Materiel } from '@/shared/domain/types'
import type {
  Besoin,
  BesoinFiltres,
  BesoinModification,
  BesoinPayload,
  Consolidation,
  DisponibiliteParams,
  HistoriqueBesoin,
  StatistiquesBesoins,
  ValidationBesoin,
} from './types'

/** Appels HTTP de ms-besoins (et lectures ms-suivi necessaires aux ecrans besoins). */
export const besoinsApi = {
  lister: (params: BesoinFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Besoin>>('/besoins', { query: { ...params }, signal }),
  detail: (id: string, signal?: AbortSignal) => http.get<Besoin>(`/besoins/${id}`, { signal }),
  historique: (id: string, signal?: AbortSignal) =>
    http.get<HistoriqueBesoin[]>(`/besoins/${id}/historique`, { signal }),
  statistiques: (signal?: AbortSignal) => http.get<StatistiquesBesoins>('/besoins/statistiques', { signal }),
  consolidation: (statut: 'EN_ATTENTE' | 'VALIDE', signal?: AbortSignal) =>
    http.get<Consolidation[]>('/besoins/consolidation', { query: { statut }, signal }),
  creer: (body: BesoinPayload) => http.post<Besoin>('/besoins', body),
  modifier: (id: string, body: BesoinModification) => http.put<Besoin>(`/besoins/${id}`, body),
  valider: (id: string, body: ValidationBesoin) => http.patch<Besoin>(`/besoins/${id}/valider`, body),
  rejeter: (id: string, motif: string) => http.patch<Besoin>(`/besoins/${id}/rejeter`, { motif }),
  orienter: (id: string) => http.post<Besoin>(`/besoins/${id}/orienter`),
  /** Chantiers selectionnables (le serveur restreint deja un chef a ses chantiers). */
  chantiers: (signal?: AbortSignal) =>
    http.get<PageResponse<Chantier>>('/chantiers', {
      query: { statut: 'EN_COURS', size: 50, sort: 'nom,asc' },
      signal,
    }),
  disponibilite: (params: DisponibiliteParams, signal?: AbortSignal) =>
    http.get<Disponibilite>('/materiels/disponibilite', { query: { ...params }, signal }),
  materiel: (id: string, signal?: AbortSignal) => http.get<Materiel>(`/materiels/${id}`, { signal }),
}
