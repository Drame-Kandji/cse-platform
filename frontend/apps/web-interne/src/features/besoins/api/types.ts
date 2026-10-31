/**
 * DTO du service ms-besoins (miroir exact de `docs/API_CONTRACT.md` §3).
 * Dates `LocalDate` en "yyyy-MM-dd", instants ISO-8601 UTC.
 */
import type { CategorieMateriel, Priorite, StatutBesoin } from '@/shared/domain/labels'
import type { IsoDate, IsoInstant } from '@/shared/domain/types'

export interface Besoin {
  readonly id: string
  /** Reference lisible, ex. "BES-2026-0001". */
  readonly reference: string
  readonly designation: string
  readonly categorie: CategorieMateriel
  readonly typeEnginId?: string | null
  readonly quantite: number
  readonly specifications?: string | null
  readonly priorite: Priorite
  readonly statut: StatutBesoin
  readonly chantierId: string
  readonly chantierNom: string
  readonly demandeurId: string
  readonly demandeurNom: string
  readonly dureeUtilisationJours: number
  readonly dateBesoin: IsoDate
  readonly justification?: string | null
  readonly motifRejet?: string | null
  readonly validateurId?: string | null
  readonly validateurNom?: string | null
  readonly dateValidation?: IsoInstant | null
  readonly materielIdsAffectes: readonly string[]
  /** Vrai si l'orientation (affectation directe / achat) n'a pas pu aboutir : ms-suivi indisponible. */
  readonly orientationEnAttente: boolean
  readonly createdAt: IsoInstant
  readonly updatedAt: IsoInstant
  readonly version: number
}

export interface HistoriqueBesoin {
  readonly id: string
  readonly statut: StatutBesoin
  readonly action: string
  readonly auteurId: string
  readonly auteurNom: string
  readonly commentaire?: string | null
  readonly date: IsoInstant
}

export interface Consolidation {
  readonly categorie: CategorieMateriel
  readonly designation: string
  readonly quantiteTotale: number
  readonly nombreDemandes: number
  readonly prioriteMax: Priorite
  readonly chantiers: readonly { readonly id: string; readonly nom: string }[]
  readonly besoinIds: readonly string[]
  readonly doublonPotentiel: boolean
}

export interface StatistiquesBesoins {
  readonly total: number
  readonly parStatut: Readonly<Partial<Record<StatutBesoin, number>>>
  readonly parPriorite: Readonly<Partial<Record<Priorite, number>>>
  readonly delaiMoyenValidationHeures?: number | null
}

/** Corps de POST /api/besoins (et PUT avec `version`). */
export interface BesoinPayload {
  readonly chantierId: string
  readonly designation: string
  readonly categorie: CategorieMateriel
  readonly typeEnginId?: string
  readonly quantite: number
  readonly specifications?: string
  readonly priorite: Priorite
  readonly dureeUtilisationJours: number
  readonly dateBesoin: IsoDate
  readonly justification?: string
}

export interface BesoinModification extends BesoinPayload {
  readonly version: number
}

/** Corps de PATCH /api/besoins/{id}/valider. */
export interface ValidationBesoin {
  readonly commentaire?: string
  readonly materielIds?: readonly string[]
}

/** Filtres de GET /api/besoins (une seule valeur par parametre ; du/au portent sur createdAt). */
export interface BesoinFiltres {
  readonly chantierId?: string
  readonly statut?: StatutBesoin
  readonly priorite?: Priorite
  readonly categorie?: CategorieMateriel
  readonly q?: string
  readonly du?: IsoDate
  readonly au?: IsoDate
}

/** Parametres de GET /api/materiels/disponibilite. */
export interface DisponibiliteParams {
  readonly categorie: CategorieMateriel
  readonly designation: string
  readonly quantite: number
  readonly du: IsoDate
  readonly au: IsoDate
}
