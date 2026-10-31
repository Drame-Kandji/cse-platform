import { http } from '@/shared/api/http'
import type { CategorieMateriel } from '@/shared/domain/labels'
import type { Affectation, IsoDate, Materiel, TypeEngin } from '@/shared/domain/types'

/** Corps de POST /api/materiels (RM). */
export interface MaterielCreation {
  readonly code: string
  readonly immatriculation?: string
  readonly designation: string
  readonly categorie: CategorieMateriel
  readonly typeEnginId?: string
  readonly marque?: string
  readonly modele?: string
  readonly tarifJournalier: number
  readonly valeurAcquisition?: number
  readonly dateAcquisition?: IsoDate
  readonly heuresFonctionnement?: number
}

/** Corps de PUT /api/materiels/{id} : champs descriptifs + verrou optimiste. */
export type MaterielModification = Omit<MaterielCreation, 'heuresFonctionnement'> & {
  readonly version: number
}

/**
 * Appels propres a la fiche engin (cycle de vie). Les lectures partagees (liste, detail, carte,
 * planning, affectation) sont dans `@/shared/suivi`.
 */
export const materielsApi = {
  creer: (body: MaterielCreation) => http.post<Materiel>('/materiels', body),
  modifier: (id: string, body: MaterielModification) => http.put<Materiel>(`/materiels/${id}`, body),
  confirmerReception: (id: string, body: { affectationId?: string; observations?: string }) =>
    http.post<Affectation>(`/materiels/${id}/confirmer-reception`, body),
  liberer: (id: string, body: { dateFinReelle?: IsoDate; commentaire?: string }) =>
    http.post<Affectation>(`/materiels/${id}/liberer`, body),
  releverHeures: (id: string, body: { heuresFonctionnement: number }) =>
    http.post<Materiel>(`/materiels/${id}/heures`, body),
  changerEtat: (id: string, body: { etat: 'DISPONIBLE' | 'EN_MAINTENANCE'; motif: string }) =>
    http.patch<Materiel>(`/materiels/${id}/etat`, body),
  demanderReforme: (id: string, body: { motif: string }) =>
    http.post<Materiel>(`/materiels/${id}/reforme/demande`, body),
  validerReforme: (id: string, body: { approuve: boolean; commentaire?: string }) =>
    http.patch<Materiel>(`/materiels/${id}/reforme/valider`, body),
  typesEngins: (signal?: AbortSignal) => http.get<TypeEngin[]>('/referentiels/types-engins', { signal }),
}
