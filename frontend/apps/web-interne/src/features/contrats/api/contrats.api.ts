import { http, type PageParams, type PageResponse } from '@/shared/api'
import type {
  ClausesRequete,
  ConfirmationLivraison,
  Contrat,
  ContratFiltres,
  FournisseurOption,
} from './types'

const base = (id: string) => `/contrats/${encodeURIComponent(id)}`

/** Appels HTTP du module contrats (ms-achats, contrat §5). */
export const contratsApi = {
  lister: (params: ContratFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Contrat>>('/contrats', { query: { ...params }, signal }),
  detail: (id: string, signal?: AbortSignal) => http.get<Contrat>(base(id), { signal }),
  enregistrerClauses: (id: string, body: ClausesRequete) => http.put<Contrat>(`${base(id)}/clauses`, body),
  signer: (id: string) => http.post<Contrat>(`${base(id)}/signer`),
  demarrerLivraison: (id: string) => http.post<Contrat>(`${base(id)}/livraison/demarrer`),
  marquerJalon: (id: string, jalonId: string, dateReelle: string) =>
    http.patch<Contrat>(`${base(id)}/jalons/${encodeURIComponent(jalonId)}`, { dateReelle }),
  confirmerLivraison: (id: string, body: ConfirmationLivraison) =>
    http.post<Contrat>(`${base(id)}/livraison/confirmer`, body),
  cloturer: (id: string) => http.post<Contrat>(`${base(id)}/cloturer`),
  fournisseurs: (signal?: AbortSignal) =>
    http.get<PageResponse<FournisseurOption>>('/fournisseurs', {
      query: { size: 50, sort: 'raisonSociale,asc' },
      signal,
    }),
}
