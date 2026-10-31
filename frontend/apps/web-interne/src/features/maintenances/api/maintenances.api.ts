import { http, type PageParams, type PageResponse } from '@/shared/api'
import type {
  ClotureMaintenance,
  Maintenance,
  MaintenanceCreation,
  MaintenanceFiltres,
  Materiel,
  Panne,
  PanneCreation,
  PanneFiltres,
} from './types'

/** Appels HTTP maintenance et pannes (ms-suivi). */
export const maintenancesApi = {
  lister: (params: MaintenanceFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Maintenance>>('/maintenances', { query: { ...params }, signal }),
  planifier: (body: MaintenanceCreation) => http.post<Maintenance>('/maintenances', body),
  demarrer: (id: string) => http.patch<Maintenance>(`/maintenances/${id}/demarrer`),
  cloturer: (id: string, body: ClotureMaintenance) =>
    http.patch<Maintenance>(`/maintenances/${id}/cloturer`, body),
  pannes: (params: PanneFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Panne>>('/pannes', { query: { ...params }, signal }),
  signaler: (body: PanneCreation) => http.post<Panne>('/pannes', body),
  /** Recherche de materiels pour les selecteurs (code, designation). */
  rechercherMateriels: (q: string, signal?: AbortSignal) =>
    http.get<PageResponse<Materiel>>('/materiels', { query: { q, size: 20, sort: 'code,asc' }, signal }),
}
