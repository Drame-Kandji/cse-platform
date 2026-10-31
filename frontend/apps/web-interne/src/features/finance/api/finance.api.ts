import { http, type PageParams, type PageResponse } from '@/shared/api'
import type {
  ContratResume,
  DecisionEngagement,
  Engagement,
  EngagementFiltres,
  Facture,
  FactureCreation,
  FactureFiltres,
  SyntheseFinance,
} from './types'

/** Appels HTTP du module finance (ms-achats). */
export const financeApi = {
  synthese: (signal?: AbortSignal) => http.get<SyntheseFinance>('/finance/synthese', { signal }),
  engagements: (params: EngagementFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Engagement>>('/engagements', { query: { ...params }, signal }),
  deciderEngagement: (contratId: string, body: DecisionEngagement) =>
    http.post<unknown>(`/engagements/${encodeURIComponent(contratId)}/valider`, body),
  factures: (params: FactureFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Facture>>('/factures', {
      query: { ...params, enRetard: params.enRetard ? true : undefined },
      signal,
    }),
  creerFacture: (body: FactureCreation) => http.post<Facture>('/factures', body),
  mandater: (id: string, referenceMandat: string) =>
    http.post<Facture>(`/factures/${encodeURIComponent(id)}/mandater`, { referenceMandat }),
  payer: (id: string) => http.post<Facture>(`/factures/${encodeURIComponent(id)}/payer`),
  rejeter: (id: string, motif: string) =>
    http.post<Facture>(`/factures/${encodeURIComponent(id)}/rejeter`, { motif }),
  contrats: (signal?: AbortSignal) =>
    http.get<PageResponse<ContratResume>>('/contrats', { query: { size: 50 }, signal }),
}
