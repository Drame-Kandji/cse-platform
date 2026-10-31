import type { EtatPlateforme } from '../types'

/** Au-dela de ce seuil, la latence est signalee comme degradee. */
export const LATENCE_LENTE_MS = 500

export type ServiceEtat = EtatPlateforme['services'][number]

/** Ton de sante d'un service (indisponible, lent, nominal). */
export function latenceTone(service: ServiceEtat): 'success' | 'warning' | 'danger' {
  if (service.statut === 'DOWN') return 'danger'
  return (service.latenceMs ?? 0) > LATENCE_LENTE_MS ? 'warning' : 'success'
}
