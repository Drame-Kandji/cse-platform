import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { PageParams } from '@/shared/api/types'
import { auditApi, plateformeApi } from '../api/admin.api'
import { auditKeys, plateformeKeys } from '../api/admin.keys'
import type { AuditFiltres } from '../types'

/** Journal d'audit filtre et pagine. */
export function useAuditLogs(params: AuditFiltres & PageParams) {
  return useQuery({
    queryKey: auditKeys.list(params),
    queryFn: ({ signal }) => auditApi.lister(params, signal),
    placeholderData: keepPreviousData,
  })
}

/** Statistiques du journal (volumes, repartition par service). */
export function useAuditStatistiques() {
  return useQuery({
    queryKey: auditKeys.custom('statistiques'),
    queryFn: ({ signal }) => auditApi.statistiques(signal),
  })
}

/**
 * Etat des services de la plateforme.
 * @param refreshMs intervalle de rafraichissement automatique (false = desactive)
 */
export function useEtatPlateforme(refreshMs: number | false) {
  return useQuery({
    queryKey: plateformeKeys.all,
    queryFn: ({ signal }) => plateformeApi.etat(signal),
    refetchInterval: refreshMs,
    refetchIntervalInBackground: false,
    staleTime: 0,
  })
}
