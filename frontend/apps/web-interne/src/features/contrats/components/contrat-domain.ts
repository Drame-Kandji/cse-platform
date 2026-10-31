import { startOfDay } from 'date-fns'
import { labelOf, STATUT_CONTRAT, type StatutContrat } from '@/shared/domain/labels'
import { toDate } from '@/shared/lib/format'
import type { Step } from '@/shared/ui/stepper'
import type { Contrat } from '../api/types'

/** Etapes du cycle de vie d'un contrat (B.3). */
export const CONTRAT_STEPS: readonly Step[] = STATUT_CONTRAT.map((statut) => ({
  id: statut,
  label: labelOf('statutContrat', statut),
}))

/** Index courant du Stepper : un contrat clos a toutes ses etapes terminees. */
export function contratStepIndex(statut: StatutContrat): number {
  if (statut === 'CLOTURE') return STATUT_CONTRAT.length
  return STATUT_CONTRAT.indexOf(statut)
}

const AVANT_LIVRAISON: readonly StatutContrat[] = ['BROUILLON', 'SIGNE', 'ENGAGEMENT_VALIDE', 'EN_LIVRAISON']

/**
 * Indique si la livraison est en retard : date prevue depassee sans livraison,
 * ou livraison effective posterieure a la date prevue.
 */
export function livraisonEnRetard(contrat: Contrat, now: Date = new Date()): boolean {
  const prevue = toDate(contrat.dateLivraisonPrevue)
  if (!prevue) return false
  const reelle = toDate(contrat.dateLivraisonReelle)
  if (reelle) return startOfDay(reelle) > startOfDay(prevue)
  return AVANT_LIVRAISON.includes(contrat.statut) && startOfDay(now) > startOfDay(prevue)
}
