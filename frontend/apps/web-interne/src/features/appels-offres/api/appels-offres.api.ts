import { downloadFile, http, type PageParams, type PageResponse } from '@/shared/api'
import type { CategorieMateriel } from '@/shared/domain/labels'
import type {
  AppelOffre,
  AppelOffreCreation,
  AppelOffreFiltres,
  BesoinAAcheter,
  Comparatif,
  ContratLie,
  DecisionApprobation,
  EvaluationOffre,
  FournisseurInvitable,
  Offre,
  OffreSaisie,
  TypeProcesVerbal,
} from './types'

const base = (id: string) => `/appels-offres/${encodeURIComponent(id)}`

/** Appels HTTP du module appels d'offres (ms-achats, contrat §5). */
export const appelsOffresApi = {
  lister: (params: AppelOffreFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<AppelOffre>>('/appels-offres', { query: { ...params }, signal }),
  detail: (id: string, signal?: AbortSignal) => http.get<AppelOffre>(base(id), { signal }),
  creer: (body: AppelOffreCreation) => http.post<AppelOffre>('/appels-offres', body),
  besoinsAAcheter: (params: PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<BesoinAAcheter>>('/besoins-a-acheter', { query: { ...params }, signal }),
  /** Fournisseurs ACTIFS d'une specialite (R14 : seuls eux peuvent etre invites). */
  fournisseursInvitables: (specialite: CategorieMateriel, signal?: AbortSignal) =>
    http.get<PageResponse<FournisseurInvitable>>('/fournisseurs', {
      query: { statut: 'ACTIF', specialite, size: 50, sort: 'scorePerformance,desc' },
      signal,
    }),
  offres: (id: string, signal?: AbortSignal) => http.get<Offre[]>(`${base(id)}/offres`, { signal }),
  saisirOffre: (id: string, body: OffreSaisie) => http.post<Offre>(`${base(id)}/offres`, body),
  cloturer: (id: string) => http.post<AppelOffre>(`${base(id)}/cloturer`),
  evaluerOffre: (id: string, offreId: string, body: EvaluationOffre) =>
    http.put<Offre>(`${base(id)}/offres/${encodeURIComponent(offreId)}/evaluation`, body),
  comparatif: (id: string, signal?: AbortSignal) =>
    http.get<Comparatif>(`${base(id)}/comparatif`, { signal }),
  attribuer: (id: string, body: { offreId: string; justification: string }) =>
    http.post<AppelOffre>(`${base(id)}/attribuer`, body),
  coValider: (id: string, body: DecisionApprobation) =>
    http.post<AppelOffre>(`${base(id)}/attribution/co-valider`, body),
  approuver: (id: string, body: DecisionApprobation) =>
    http.post<AppelOffre>(`${base(id)}/attribution/approuver`, body),
  /**
   * Contrat genere a l'attribution (R03). Ni AppelOffre ni Attribution n'exposent son id et
   * `GET /contrats?q` cherche dans numero/objet/fournisseurNom : on cherche par objet (repris
   * de l'AO) puis on filtre sur `appelOffreId`.
   */
  contratLie: async (ao: Pick<AppelOffre, 'id' | 'objet'>, signal?: AbortSignal) => {
    const page = await http.get<PageResponse<ContratLie>>('/contrats', {
      query: { q: ao.objet, size: 50 },
      signal,
    })
    return page.content.find((c) => c.appelOffreId === ao.id) ?? null
  },
  procesVerbal: (id: string, reference: string, type: TypeProcesVerbal) =>
    downloadFile(`${base(id)}/proces-verbaux/${type}`, `PV-${type}-${reference}.pdf`),
}
