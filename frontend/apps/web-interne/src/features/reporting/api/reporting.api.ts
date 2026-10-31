import { downloadFile } from '@/shared/api/download'
import { http } from '@/shared/api/http'
import type { PageResponse } from '@/shared/api/types'
import type { ExportFormat, Point, SeriesParams, Tableau, TableauParams } from '../types'

/** Chantier (champs utiles au filtre). */
export interface ChantierOption {
  readonly id: string
  readonly code: string
  readonly nom: string
}

export const reportingApi = {
  chantiers: (signal?: AbortSignal) =>
    http.get<PageResponse<ChantierOption>>('/chantiers', { query: { size: 50, sort: 'nom,asc' }, signal }),
  kpis: (params: TableauParams, signal?: AbortSignal) =>
    http.get<Tableau>('/reporting/kpis', { query: { ...params }, signal }),
  series: (params: SeriesParams, signal?: AbortSignal) =>
    http.get<Point[]>('/reporting/series', { query: { ...params }, signal }),
  exporter: (format: ExportFormat, params: TableauParams) =>
    downloadFile('/reporting/export', `tableau-de-bord.${format}`, { query: { format, ...params } }),
}
