/**
 * DTO chantiers propres au module (contrat §2). Le DTO `Chantier` lui-meme est partage :
 * `@/shared/domain/types`.
 */
import type { StatutChantier } from '@/shared/domain/labels'
import type { IsoDate } from '@/shared/domain/types'

export type { Chantier } from '@/shared/domain/types'

/** Corps de POST /api/chantiers. */
export interface ChantierPayload {
  readonly code: string
  readonly nom: string
  readonly localisation: string
  readonly region: string
  readonly latitude: number
  readonly longitude: number
  readonly budget: number
  readonly montantMarche: number
  readonly dateDebut: IsoDate
  readonly dateFinPrevue?: IsoDate
  readonly chefChantierId?: string
  readonly chefChantierNom?: string
}

/** Corps de PUT /api/chantiers/{id} (verrou optimiste). */
export interface ChantierModification extends ChantierPayload {
  readonly version: number
}

export interface ChantierFiltres {
  readonly statut?: StatutChantier
  readonly q?: string
}

/** Compte chef de chantier (GET /api/admin/utilisateurs, reserve ADMIN). */
export interface ChefChantierOption {
  readonly id: string
  readonly prenom: string
  readonly nom: string
  readonly email: string
}
