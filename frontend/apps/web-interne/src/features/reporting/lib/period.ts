import { differenceInCalendarDays, isValid, parseISO, subDays } from 'date-fns'
import { toIsoDate } from '@/shared/lib/format'
import type { Granularite } from '../types'

export const PERIOD_PRESETS = [
  { value: '30', label: '30 jours', days: 30 },
  { value: '90', label: '90 jours', days: 90 },
  { value: '365', label: '12 mois', days: 365 },
] as const

export type PeriodPreset = (typeof PERIOD_PRESETS)[number]['value'] | 'custom'

export interface Period {
  readonly preset: PeriodPreset
  /** "yyyy-MM-dd". */
  readonly du: string
  readonly au: string
}

const DEFAULT_PRESET = '90'

function parseDay(value: string | null): Date | null {
  if (!value) return null
  const date = parseISO(value)
  return isValid(date) ? date : null
}

/**
 * Periode d'analyse a partir des parametres d'URL (`periode=30|90|365|custom&du&au`).
 * Defaut : 90 derniers jours (aligne sur ms-reporting).
 */
export function periodFromParams(params: URLSearchParams, today = new Date()): Period {
  const preset = params.get('periode')
  const du = parseDay(params.get('du'))
  const au = parseDay(params.get('au'))
  if (preset === 'custom' && du && au && du <= au) {
    return { preset: 'custom', du: toIsoDate(du) ?? '', au: toIsoDate(au) ?? '' }
  }
  const found = PERIOD_PRESETS.find((item) => item.value === preset) ?? PERIOD_PRESETS[1]
  return {
    preset: found.value,
    du: toIsoDate(subDays(today, found.days - 1)) ?? '',
    au: toIsoDate(today) ?? '',
  }
}

/** Parametres d'URL d'une periode (le preset par defaut est omis). */
export function periodToParams(period: { preset: PeriodPreset; du?: string; au?: string }) {
  if (period.preset === 'custom') return { periode: 'custom', du: period.du ?? null, au: period.au ?? null }
  return { periode: period.preset === DEFAULT_PRESET ? null : period.preset, du: null, au: null }
}

/** Granularite adaptee a la longueur de la periode (lisibilite des courbes). */
export function granulariteFor(period: Pick<Period, 'du' | 'au'>): Granularite {
  const du = parseDay(period.du)
  const au = parseDay(period.au)
  const days = du && au ? differenceInCalendarDays(au, du) + 1 : 90
  if (days <= 45) return 'JOUR'
  if (days <= 180) return 'SEMAINE'
  return 'MOIS'
}
