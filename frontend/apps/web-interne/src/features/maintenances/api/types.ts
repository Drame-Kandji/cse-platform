/**
 * DTO maintenance / pannes propres au module (contrat §2). `Maintenance`, `Panne` et `PanneCreation`
 * sont partages : `@/shared/domain/types`.
 */
import type { StatutMaintenance, StatutPanne, TypeMaintenance } from '@/shared/domain/labels'
import type { IsoDate } from '@/shared/domain/types'

export type { Maintenance, Materiel, Panne, PanneCreation } from '@/shared/domain/types'

/** Corps de POST /api/maintenances. */
export interface MaintenanceCreation {
  readonly materielId: string
  readonly type: TypeMaintenance
  readonly description: string
  readonly datePlanifiee: IsoDate
  readonly technicien?: string
  /** Panne a l'origine de l'ordre de travail. */
  readonly panneId?: string
}

/** Corps de PATCH /api/maintenances/{id}/cloturer. */
export interface ClotureMaintenance {
  readonly cout: number
  readonly observations?: string
  readonly heuresCompteur?: number
}

export interface MaintenanceFiltres {
  readonly statut?: StatutMaintenance
  readonly type?: TypeMaintenance
  readonly materielId?: string
}

export interface PanneFiltres {
  readonly statut?: StatutPanne
  readonly chantierId?: string
  readonly materielId?: string
}
