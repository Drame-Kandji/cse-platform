import { keepPreviousData, useMutation, useQueries, useQuery } from '@tanstack/react-query'
import { saveBlob } from '@/shared/api/download'
import { queuesApi } from '../api/queues.api'
import { reportingApi } from '../api/reporting.api'
import { reportingKeys } from '../api/reporting.keys'
import type { QueueDef } from '../lib/queues'
import type { ExportFormat, Indicateur, Point, SeriesParams, TableauParams } from '../types'

/** Tableau de bord d'un profil sur une periode (sections presentes selon le profil). */
export function useTableau(params: TableauParams, options: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: reportingKeys.custom('kpis', params),
    queryFn: ({ signal }) => reportingApi.kpis(params, signal),
    placeholderData: keepPreviousData,
    enabled: options.enabled ?? true,
  })
}

/** Serie temporelle d'un indicateur. */
export function useSerie(params: SeriesParams, options: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: reportingKeys.custom('series', params),
    queryFn: ({ signal }) => reportingApi.series(params, signal),
    placeholderData: keepPreviousData,
    enabled: options.enabled ?? true,
  })
}

/**
 * Plusieurs series en parallele (sparklines, graphiques combines), indexees par indicateur.
 * Une serie en erreur (indicateur non autorise pour le profil) est simplement absente.
 */
export function useSeries(indicateurs: readonly Indicateur[], base: Omit<SeriesParams, 'indicateur'>) {
  const results = useQueries({
    queries: indicateurs.map((indicateur) => {
      const params: SeriesParams = { ...base, indicateur }
      return {
        queryKey: reportingKeys.custom('series', params),
        queryFn: ({ signal }: { signal: AbortSignal }) => reportingApi.series(params, signal),
        placeholderData: keepPreviousData,
      }
    }),
  })
  const points: Partial<Record<Indicateur, readonly Point[]>> = {}
  indicateurs.forEach((indicateur, index) => {
    const data = results[index]?.data
    if (data) points[indicateur] = data
  })
  return { points, isPending: results.some((result) => result.isPending) }
}

/** Compteurs des files "A traiter" (une erreur = compteur indisponible, sans bloquer la carte). */
export function useQueueCounts(queues: readonly QueueDef[]) {
  return useQueries({
    queries: queues.map((queue) => ({
      queryKey: reportingKeys.custom('queue', queue.source),
      queryFn: ({ signal }: { signal: AbortSignal }) => queuesApi[queue.source](signal),
      staleTime: 60_000,
    })),
  })
}

/** Export PDF / Excel du tableau de bord (nom de fichier issu de Content-Disposition). */
export function useExportTableau() {
  return useMutation({
    mutationFn: async ({ format, params }: { format: ExportFormat; params: TableauParams }) => {
      const file = await reportingApi.exporter(format, params)
      saveBlob(file.blob, file.filename)
      return file.filename
    },
    meta: { successMessage: 'Export téléchargé' },
  })
}
