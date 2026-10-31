import type { Materiel } from '@/shared/domain/types'
import { formatNumber } from '@/shared/lib/format'

/** Ratio heures depuis maintenance / seuil (null si aucun seuil n'est defini). */
export function maintenanceRatio(
  materiel: Pick<Materiel, 'heuresDepuisMaintenance' | 'seuilMaintenanceHeures'>,
): number | null {
  const seuil = materiel.seuilMaintenanceHeures
  if (!seuil || seuil <= 0) return null
  return materiel.heuresDepuisMaintenance / seuil
}

/** Normalise une saisie numerique ("12 500" -> "12500"). */
export function normaliserEntier(saisie: string): string {
  return saisie.replace(/\s/g, '')
}

/**
 * Valide un releve de compteur : entier, superieur ou egal au compteur actuel.
 * @returns message d'erreur francais, ou null si la saisie est valide
 */
export function validerReleve(saisie: string, compteurActuel: number): string | null {
  const value = normaliserEntier(saisie)
  if (!/^\d+$/.test(value)) return 'Saisissez un nombre entier d’heures'
  if (Number(value) < compteurActuel) {
    return `Le compteur ne peut pas diminuer (actuel : ${formatNumber(compteurActuel)} h)`
  }
  return null
}
