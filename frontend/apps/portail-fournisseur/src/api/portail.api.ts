import { http } from '@/shared/api/http'
import type { PageResponse } from '@/shared/api/types'
import { estIntrouvable } from '@/domain/errors'
import type {
  AppelOffrePortail,
  Contrat,
  Facture,
  FactureDepot,
  Inscription,
  InscriptionRecue,
  LigneCatalogue,
  LigneCatalogueSaisie,
  OffreDepot,
  OffrePortail,
  ProfilFournisseur,
  ProfilModification,
} from '@/domain/types'

/** Volume d'un fournisseur : une page de 50 (maximum backend) suffit aux listes du portail. */
export const TAILLE_LISTE = 50

/** Page vide (endpoint absent ou liste vide). */
export function pageVide<T>(): PageResponse<T> {
  return {
    content: [],
    page: 0,
    size: TAILLE_LISTE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  }
}

/**
 * Liste tolerante : un 404 (route pas encore deployee par la passerelle) devient une page vide pour
 * que l'ecran affiche son etat vide au lieu d'une erreur. Les autres erreurs remontent.
 */
export async function listeTolerante<T>(promise: Promise<PageResponse<T>>): Promise<PageResponse<T>> {
  try {
    return await promise
  } catch (error) {
    if (estIntrouvable(error)) return pageVide<T>()
    throw error
  }
}

const liste = { page: 0, size: TAILLE_LISTE }

/** Appels HTTP du portail fournisseur (`/api/portail/**`, contrat d'API §13). */
export const portailApi = {
  appelsOffres: (signal?: AbortSignal) =>
    listeTolerante(
      http.get<PageResponse<AppelOffrePortail>>('/portail/appels-offres', {
        query: { ...liste, sort: 'dateLimite,asc' },
        signal,
      }),
    ),
  appelOffre: (id: string, signal?: AbortSignal) =>
    http.get<AppelOffrePortail>(`/portail/appels-offres/${id}`, { signal }),
  deposerOffre: (id: string, body: OffreDepot) =>
    http.post<OffrePortail>(`/portail/appels-offres/${id}/offres`, body),
  mesOffres: (signal?: AbortSignal) =>
    listeTolerante(
      http.get<PageResponse<OffrePortail>>('/portail/offres/mes-offres', { query: liste, signal }),
    ),

  contrats: (signal?: AbortSignal) =>
    listeTolerante(http.get<PageResponse<Contrat>>('/portail/contrats', { query: liste, signal })),
  contrat: (id: string, signal?: AbortSignal) => http.get<Contrat>(`/portail/contrats/${id}`, { signal }),
  signer: (id: string) => http.post<Contrat>(`/portail/contrats/${id}/signer`),
  refuser: (id: string, motif: string) => http.post<Contrat>(`/portail/contrats/${id}/refuser`, { motif }),
  expedier: (id: string, body: { dateExpedition: string; commentaire?: string }) =>
    http.post<Contrat>(`/portail/contrats/${id}/expedier`, body),
  jalonAtteint: (id: string, jalonId: string, dateReelle: string) =>
    http.patch<Contrat>(`/portail/contrats/${id}/jalons/${jalonId}`, { dateReelle }),

  factures: (signal?: AbortSignal) =>
    listeTolerante(http.get<PageResponse<Facture>>('/portail/factures', { query: liste, signal })),
  deposerFacture: (body: FactureDepot) => http.post<Facture>('/portail/factures', body),

  profil: (signal?: AbortSignal) => http.get<ProfilFournisseur>('/portail/profil', { signal }),
  modifierProfil: (body: ProfilModification) => http.put<ProfilFournisseur>('/portail/profil', body),
  catalogue: (signal?: AbortSignal) => http.get<LigneCatalogue[]>('/portail/catalogue', { signal }),
  publierLigne: (body: LigneCatalogueSaisie) => http.put<LigneCatalogue>('/portail/catalogue', body),

  inscrire: (body: Inscription) => http.post<InscriptionRecue>('/portail/inscription', body),
}
