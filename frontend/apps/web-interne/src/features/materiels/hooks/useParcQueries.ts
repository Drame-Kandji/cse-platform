import { useQueries, useQuery } from '@tanstack/react-query'
import { ETAT_MATERIEL, type EtatMateriel } from '@/shared/domain/labels'
import { materielKeys, suiviApi, type MaterielFiltres } from '@/shared/suivi'
import { materielsApi } from '../api/materiels.api'

export interface EtatCount {
  readonly etat: EtatMateriel
  readonly count: number | undefined
}

/**
 * Effectifs par etat pour les filtres courants (hors filtre d'etat) : une requete `size=1`
 * par etat, lecture de `totalElements`. Evite un endpoint d'agregat non prevu au contrat.
 */
export function useEtatSummary(filtres: Omit<MaterielFiltres, 'etat'>) {
  const results = useQueries({
    queries: ETAT_MATERIEL.map((etat) => {
      const params = { ...filtres, etat, page: 0, size: 1 }
      return {
        queryKey: materielKeys.custom('summary', params),
        queryFn: ({ signal }: { signal: AbortSignal }) => suiviApi.materiels(params, signal),
        select: (page: { totalElements: number }) => page.totalElements,
      }
    }),
  })
  const counts: EtatCount[] = ETAT_MATERIEL.map((etat, index) => ({
    etat,
    count: results[index]?.data,
  }))
  return {
    counts,
    isPending: results.some((result) => result.isPending),
    total: counts.reduce((sum, item) => sum + (item.count ?? 0), 0),
  }
}

/** Referentiel des types d'engins (formulaire materiel). */
export function useTypesEngins() {
  return useQuery({
    queryKey: materielKeys.custom('types-engins'),
    queryFn: ({ signal }) => materielsApi.typesEngins(signal),
    staleTime: 10 * 60_000,
  })
}
