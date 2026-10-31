import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import { materielKeys, suiviApi, type CarteFiltres } from '@/shared/suivi'
import { grouperParPosition } from '../lib/grouping'

/** Donnees GeoJSON de la carte du parc, regroupees par position (chantiers + materiels). */
export function useCarte(filtres: CarteFiltres) {
  const query = useQuery({
    queryKey: materielKeys.custom('carte', filtres),
    queryFn: ({ signal }) => suiviApi.carte(filtres, signal),
    placeholderData: keepPreviousData,
  })
  const groupes = useMemo(() => grouperParPosition(query.data?.features ?? []), [query.data])
  return { ...query, groupes }
}
