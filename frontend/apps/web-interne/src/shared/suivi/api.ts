import { http } from '@/shared/api/http'
import type { PageParams, PageResponse } from '@/shared/api/types'
import type { CategorieMateriel, EtatMateriel, StatutBesoin, StatutChantier } from '@/shared/domain/labels'
import type {
  Affectation,
  CarteFeatureCollection,
  Chantier,
  IsoDate,
  Materiel,
  MaterielDetail,
  Panne,
  PanneCreation,
} from '@/shared/domain/types'

/** Filtres de GET /api/materiels (etat et categorie : une seule valeur, cote backend). */
export interface MaterielFiltres {
  readonly etat?: EtatMateriel
  readonly categorie?: CategorieMateriel
  readonly chantierId?: string
  readonly q?: string
  readonly maintenanceDue?: boolean
}

/** Corps de POST /api/materiels/{id}/affecter. */
export interface AffectationCreation {
  readonly chantierId: string
  readonly besoinId?: string
  readonly dateDebut: IsoDate
  readonly dateFinPrevue: IsoDate
}

/** Filtres de GET /api/affectations/planning (periode <= 366 jours). */
export interface PlanningFiltres {
  readonly du: IsoDate
  readonly au: IsoDate
  readonly chantierId?: string
  readonly categorie?: CategorieMateriel
}

/** Filtres de GET /api/materiels/carte. */
export interface CarteFiltres {
  readonly categorie?: CategorieMateriel
  readonly etat?: EtatMateriel
}

/** Sous-ensemble d'un besoin utile pour le rattacher a une affectation. */
export interface BesoinOption {
  readonly id: string
  readonly reference: string
  readonly designation: string
  readonly quantite: number
  readonly statut: StatutBesoin
}

/** Appels HTTP de ms-suivi partages entre modules (parc, carte, planning, maintenance). */
export const suiviApi = {
  materiels: (params: MaterielFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Materiel>>('/materiels', { query: { ...params }, signal }),
  materiel: (id: string, signal?: AbortSignal) => http.get<MaterielDetail>(`/materiels/${id}`, { signal }),
  affecter: (materielId: string, body: AffectationCreation) =>
    http.post<Affectation>(`/materiels/${materielId}/affecter`, body),
  carte: (params: CarteFiltres, signal?: AbortSignal) =>
    http.get<CarteFeatureCollection>('/materiels/carte', { query: { ...params }, signal }),
  planning: (params: PlanningFiltres, signal?: AbortSignal) =>
    http.get<Affectation[]>('/affectations/planning', { query: { ...params }, signal }),
  chantiers: (params: { statut?: StatutChantier; q?: string } & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Chantier>>('/chantiers', { query: { ...params }, signal }),
  besoins: (params: { chantierId: string; statut: StatutBesoin; size: number }, signal?: AbortSignal) =>
    http.get<PageResponse<BesoinOption>>('/besoins', { query: { ...params }, signal }),
  signalerPanne: (body: PanneCreation) => http.post<Panne>('/pannes', body),
}
