import type { Priorite } from '@/shared/domain/labels'
import type { Besoin } from '../api/types'

const RANG: Readonly<Record<Priorite, number>> = { CRITIQUE: 0, URGENT: 1, NORMAL: 2 }

/**
 * Ordre de la file de validation : priorite decroissante puis anciennete (le plus ancien d'abord).
 * Fait cote client : un tri serveur sur l'enum serait alphabetique.
 */
export function trierParPrioriteEtAnciennete<T extends Pick<Besoin, 'priorite' | 'createdAt'>>(
  besoins: readonly T[],
): T[] {
  return [...besoins].sort(
    (a, b) => RANG[a.priorite] - RANG[b.priorite] || a.createdAt.localeCompare(b.createdAt),
  )
}
