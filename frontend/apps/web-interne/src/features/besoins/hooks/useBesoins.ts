import { keepPreviousData, useQueries, useQuery } from '@tanstack/react-query'
import type { PageParams } from '@/shared/api'
import { besoinsApi } from '../api/besoins.api'
import { besoinKeys } from '../api/besoins.keys'
import type { BesoinFiltres, DisponibiliteParams } from '../api/types'

/** Liste paginee des besoins (filtres + pagination serveur). */
export function useBesoins(params: BesoinFiltres & PageParams, options: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: besoinKeys.list(params),
    queryFn: ({ signal }) => besoinsApi.lister(params, signal),
    placeholderData: keepPreviousData,
    enabled: options.enabled,
  })
}

/** Detail d'un besoin. */
export function useBesoin(id: string | undefined) {
  return useQuery({
    queryKey: besoinKeys.detail(id ?? ''),
    queryFn: ({ signal }) => besoinsApi.detail(id ?? '', signal),
    enabled: Boolean(id),
  })
}

/** Historique chronologique des changements de statut d'un besoin. */
export function useHistoriqueBesoin(id: string | undefined) {
  return useQuery({
    queryKey: besoinKeys.custom('historique', id ?? ''),
    queryFn: ({ signal }) => besoinsApi.historique(id ?? '', signal),
    enabled: Boolean(id),
  })
}

/** Statistiques globales (le serveur filtre deja pour un chef de chantier). */
export function useStatistiquesBesoins() {
  return useQuery({
    queryKey: besoinKeys.custom('statistiques'),
    queryFn: ({ signal }) => besoinsApi.statistiques(signal),
  })
}

/** Regroupement des besoins similaires et doublons potentiels (RM). */
export function useConsolidation(statut: 'EN_ATTENTE' | 'VALIDE', enabled = true) {
  return useQuery({
    queryKey: besoinKeys.custom('consolidation', statut),
    queryFn: ({ signal }) => besoinsApi.consolidation(statut, signal),
    enabled,
  })
}

/** Chantiers en cours selectionnables pour un besoin (deja restreints cote serveur pour un chef). */
export function useChantiersSelectionnables() {
  return useQuery({
    queryKey: ['chantiers', 'list', { statut: 'EN_COURS', size: 50, sort: 'nom,asc' }] as const,
    queryFn: ({ signal }) => besoinsApi.chantiers(signal),
    staleTime: 5 * 60_000,
  })
}

/** Disponibilite du parc pour un besoin (aide a la validation). */
export function useDisponibilite(params: DisponibiliteParams | null) {
  return useQuery({
    queryKey: ['materiels', 'disponibilite', params] as const,
    queryFn: ({ signal }) => besoinsApi.disponibilite(params as DisponibiliteParams, signal),
    enabled: params !== null,
  })
}

/** Fiches des materiels affectes a un besoin (libelles des liens). */
export function useMaterielsAffectes(ids: readonly string[]) {
  return useQueries({
    queries: ids.map((id) => ({
      queryKey: ['materiels', 'detail', id] as const,
      queryFn: ({ signal }: { signal: AbortSignal }) => besoinsApi.materiel(id, signal),
      staleTime: 5 * 60_000,
    })),
  })
}
