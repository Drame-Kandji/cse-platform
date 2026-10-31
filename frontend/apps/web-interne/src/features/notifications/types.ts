import type { NiveauNotification } from '@/shared/domain/labels'

/** Notification destinee a l'utilisateur ou a l'un de ses roles (contrat §7). */
export interface Notification {
  readonly id: string
  /** Type d'evenement source (ex. BESOIN_VALIDE). */
  readonly type: string
  readonly titre: string
  readonly message: string
  readonly niveau: NiveauNotification
  /** Route front, ex. "/besoins/<id>". */
  readonly lien?: string | null
  readonly entite?: string | null
  readonly entiteId?: string | null
  readonly lue: boolean
  readonly createdAt: string
}

export interface NotificationFiltres {
  readonly lue?: boolean
  readonly page?: number
  readonly size?: number
}

export interface NonLuesCount {
  readonly count: number
}
