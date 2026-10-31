import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { PageParams } from '@/shared/api'
import { contratsApi } from '../api/contrats.api'
import { contratKeys } from '../api/contrats.keys'
import type { ContratFiltres } from '../api/types'

/** Liste paginee des contrats, filtree par statut, fournisseur et recherche. */
export function useContrats(params: ContratFiltres & PageParams) {
  return useQuery({
    queryKey: contratKeys.list(params),
    queryFn: ({ signal }) => contratsApi.lister(params, signal),
    placeholderData: keepPreviousData,
  })
}

/** Detail d'un contrat (clauses, jalons, engagement). */
export function useContrat(id: string) {
  return useQuery({
    queryKey: contratKeys.detail(id),
    queryFn: ({ signal }) => contratsApi.detail(id, signal),
  })
}

/** Fournisseurs proposes dans le filtre de la liste (50 premiers, ordre alphabetique). */
export function useFournisseursOptions() {
  return useQuery({
    queryKey: contratKeys.custom('fournisseurs-options'),
    queryFn: ({ signal }) => contratsApi.fournisseurs(signal),
    staleTime: 5 * 60_000,
  })
}
