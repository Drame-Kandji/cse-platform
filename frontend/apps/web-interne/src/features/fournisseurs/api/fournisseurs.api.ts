import { http, type PageParams, type PageResponse } from '@/shared/api'
import type { CategorieMateriel, StatutLitige } from '@/shared/domain/labels'
import type {
  Classement,
  ComparaisonPrix,
  DecisionBlacklistage,
  DocumentAdministratif,
  DocumentSaisie,
  Evaluation,
  EvaluationSaisie,
  Fournisseur,
  FournisseurDetail,
  FournisseurFiltres,
  FournisseurModification,
  FournisseurSaisie,
  LigneCatalogue,
  LigneCatalogueSaisie,
  Litige,
  LitigeActionSaisie,
  LitigeSaisie,
  PrixHistorique,
  StatistiquesFournisseurs,
} from './types'

const base = '/fournisseurs'
const one = (id: string) => `${base}/${encodeURIComponent(id)}`

/** Appels HTTP ms-fournisseurs (contrat §4). */
export const fournisseursApi = {
  lister: (params: FournisseurFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Fournisseur>>(base, { query: { ...params }, signal }),
  detail: (id: string, signal?: AbortSignal) => http.get<FournisseurDetail>(one(id), { signal }),
  statistiques: (signal?: AbortSignal) =>
    http.get<StatistiquesFournisseurs>(`${base}/statistiques`, { signal }),
  classement: (categorie: CategorieMateriel | undefined, signal?: AbortSignal) =>
    http.get<Classement[]>(`${base}/classement`, { query: { categorie }, signal }),
  comparaisonPrix: (params: { categorie?: CategorieMateriel; q?: string }, signal?: AbortSignal) =>
    http.get<ComparaisonPrix[]>(`${base}/comparaison-prix`, { query: params, signal }),
  creer: (body: FournisseurSaisie) => http.post<FournisseurDetail>(base, body),
  modifier: (id: string, body: FournisseurModification) =>
    http.put<FournisseurDetail>(one(id), body),
  validerInscription: (id: string) => http.patch<Fournisseur>(`${one(id)}/valider-inscription`),
  suspendre: (id: string, motif: string) => http.patch<Fournisseur>(`${one(id)}/suspendre`, { motif }),
  reactiver: (id: string) => http.patch<Fournisseur>(`${one(id)}/reactiver`),
  demanderBlacklistage: (id: string, motif: string) =>
    http.post<Fournisseur>(`${one(id)}/blacklistage/demande`, { motif }),
  deciderBlacklistage: (id: string, body: DecisionBlacklistage) =>
    http.patch<Fournisseur>(`${one(id)}/blacklister`, body),
  ajouterDocument: (id: string, body: DocumentSaisie) =>
    http.post<DocumentAdministratif>(`${one(id)}/documents`, body),
  evaluer: (id: string, body: EvaluationSaisie) => http.post<Evaluation>(`${one(id)}/evaluer`, body),
  evaluations: (id: string, signal?: AbortSignal) =>
    http.get<Evaluation[]>(`${one(id)}/evaluations`, { signal }),
  catalogue: (id: string, signal?: AbortSignal) =>
    http.get<LigneCatalogue[]>(`${one(id)}/catalogue`, { signal }),
  enregistrerLigne: (id: string, body: LigneCatalogueSaisie) =>
    http.post<LigneCatalogue>(`${one(id)}/catalogue`, body),
  historiquePrix: (id: string, reference: string | undefined, signal?: AbortSignal) =>
    http.get<PrixHistorique[]>(`${one(id)}/prix/historique`, { query: { reference }, signal }),
  litiges: (params: { fournisseurId?: string; statut?: StatutLitige } & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Litige>>('/litiges', { query: { ...params }, signal }),
  ouvrirLitige: (id: string, body: LitigeSaisie) => http.post<Litige>(`${one(id)}/litiges`, body),
  agirLitige: (litigeId: string, body: LitigeActionSaisie) =>
    http.patch<Litige>(`/litiges/${encodeURIComponent(litigeId)}/action`, body),
}
