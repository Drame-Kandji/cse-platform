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
   * Contrat genere a l'attribution (R03), par ordre de preference :
   * 1. `AppelOffre.contratId` (lien direct, aucun appel) ;
   * 2. `GET /contrats?appelOffreId=` (filtre serveur ; un serveur plus ancien l'ignore, d'ou la
   *    verification de `appelOffreId` sur chaque ligne) ;
   * 3. repli historique : recherche plein texte sur l'objet de l'AO puis filtre sur `appelOffreId`.
   */
  contratLie: async (
    ao: Pick<AppelOffre, 'id' | 'objet' | 'contratId'>,
    signal?: AbortSignal,
  ): Promise<ContratLie | null> => {
    if (ao.contratId) return { id: ao.contratId, appelOffreId: ao.id }
    const lie = (page: PageResponse<ContratLie>) => page.content.find((c) => c.appelOffreId === ao.id) ?? null
    const parFiltre = lie(
      await http.get<PageResponse<ContratLie>>('/contrats', {
        query: { appelOffreId: ao.id, size: 50 },
        signal,
      }),
    )
    if (parFiltre) return parFiltre
    return lie(
      await http.get<PageResponse<ContratLie>>('/contrats', {
        query: { q: ao.objet, size: 50 },
        signal,
      }),
    )
  },
  procesVerbal: (id: string, reference: string, type: TypeProcesVerbal) =>
    downloadFile(`${base(id)}/proces-verbaux/${type}`, `PV-${type}-${reference}.pdf`),
}
