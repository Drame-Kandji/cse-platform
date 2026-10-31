import {
  addDays,
  addMonths,
  addQuarters,
  addWeeks,
  differenceInCalendarDays,
  eachDayOfInterval,
  eachWeekOfInterval,
  endOfMonth,
  endOfQuarter,
  format,
  isWeekend,
  max as maxDate,
  min as minDate,
  startOfDay,
  startOfMonth,
  startOfQuarter,
  startOfWeek,
} from 'date-fns'
import { fr } from 'date-fns/locale'
import { toDate } from '@/shared/lib/format'

/** Niveau de zoom du planning. */
export type Zoom = 'semaine' | 'mois' | 'trimestre'
export const ZOOMS: readonly Zoom[] = ['semaine', 'mois', 'trimestre']

/** Periode affichee, bornes incluses (jours calendaires). */
export interface Periode {
  readonly du: Date
  readonly au: Date
}

/** Largeur minimale d'un jour (px) par zoom : au-dela, defilement horizontal (mobile). */
export const PX_PAR_JOUR: Readonly<Record<Zoom, number>> = { semaine: 110, mois: 30, trimestre: 10 }

const WEEK = { weekStartsOn: 1 } as const

/** Periode contenant `ancre` pour un zoom (semaine du lundi, mois ou trimestre civil). */
export function periodeDe(zoom: Zoom, ancre: Date): Periode {
  const day = startOfDay(ancre)
  if (zoom === 'semaine') {
    const du = startOfWeek(day, WEEK)
    return { du, au: addDays(du, 6) }
  }
  if (zoom === 'mois') return { du: startOfMonth(day), au: startOfDay(endOfMonth(day)) }
  return { du: startOfQuarter(day), au: startOfDay(endOfQuarter(day)) }
}

/** Decale l'ancre d'une periode (precedente : -1, suivante : +1). */
export function decaler(zoom: Zoom, ancre: Date, sens: number): Date {
  if (zoom === 'semaine') return addWeeks(ancre, sens)
  if (zoom === 'mois') return addMonths(ancre, sens)
  return addQuarters(ancre, sens)
}

/** Nombre de jours de la periode (bornes incluses). */
export function nombreDeJours(periode: Periode): number {
  return differenceInCalendarDays(periode.au, periode.du) + 1
}

/** Position horizontale d'une barre, en pourcentage de la periode. */
export interface PositionBarre {
  readonly left: number
  readonly width: number
  /** La barre commence avant la periode (bord gauche coupe). */
  readonly coupeeAvant: boolean
  /** La barre se termine apres la periode (bord droit coupe). */
  readonly coupeeApres: boolean
}

/**
 * Calcule la position d'une affectation [debut, fin] (jours inclus) dans la periode.
 * @returns null si l'affectation est hors periode ou incoherente (fin < debut)
 * @example positionBarre('2026-10-05', '2026-10-11', { du: 1 oct., au: 31 oct. }) // left 12.9 %, width 22.6 %
 */
export function positionBarre(
  debut: Date | string,
  fin: Date | string,
  periode: Periode,
): PositionBarre | null {
  const start = toDate(debut)
  const end = toDate(fin)
  if (!start || !end) return null
  const s = startOfDay(start)
  const e = startOfDay(end)
  if (e < s || e < periode.du || s > periode.au) return null
  const visibleStart = maxDate([s, periode.du])
  const visibleEnd = minDate([e, periode.au])
  const total = nombreDeJours(periode)
  return {
    left: (differenceInCalendarDays(visibleStart, periode.du) / total) * 100,
    width: ((differenceInCalendarDays(visibleEnd, visibleStart) + 1) / total) * 100,
    coupeeAvant: s < periode.du,
    coupeeApres: e > periode.au,
  }
}

/** Position (%) du milieu du jour `jour`, ou null s'il est hors periode (marqueur "aujourd'hui"). */
export function positionJour(jour: Date, periode: Periode): number | null {
  const day = startOfDay(jour)
  if (day < periode.du || day > periode.au) return null
  return ((differenceInCalendarDays(day, periode.du) + 0.5) / nombreDeJours(periode)) * 100
}

/** Jour correspondant a une abscisse relative (0..1) dans la periode (clic sur un creneau vide). */
export function jourAuPoint(ratio: number, periode: Periode): Date {
  const total = nombreDeJours(periode)
  const index = Math.min(total - 1, Math.max(0, Math.floor(ratio * total)))
  return addDays(periode.du, index)
}

/** Graduation de l'en-tete temporel. */
export interface Graduation {
  readonly key: string
  readonly label: string
  readonly sousLabel?: string
  readonly left: number
  readonly width: number
  readonly weekend: boolean
}

/** Graduations : jours (semaine, mois) ou semaines (trimestre). */
export function graduations(zoom: Zoom, periode: Periode): Graduation[] {
  const total = nombreDeJours(periode)
  if (zoom === 'trimestre') {
    return eachWeekOfInterval({ start: periode.du, end: periode.au }, WEEK).map((lundi) => {
      const start = maxDate([lundi, periode.du])
      const end = minDate([addDays(lundi, 6), periode.au])
      return {
        key: lundi.toISOString(),
        label: format(start, 'd MMM', { locale: fr }),
        sousLabel: `S${format(lundi, 'II')}`,
        left: (differenceInCalendarDays(start, periode.du) / total) * 100,
        width: ((differenceInCalendarDays(end, start) + 1) / total) * 100,
        weekend: false,
      }
    })
  }
  return eachDayOfInterval({ start: periode.du, end: periode.au }).map((day, index) => ({
    key: day.toISOString(),
    label: zoom === 'semaine' ? format(day, 'EEEE d', { locale: fr }) : format(day, 'd'),
    sousLabel: zoom === 'mois' ? format(day, 'EEEEE', { locale: fr }) : undefined,
    left: (index / total) * 100,
    width: 100 / total,
    weekend: isWeekend(day),
  }))
}

/** Libelle de la periode : "Semaine du 26 oct. 2026", "octobre 2026", "T4 2026". */
export function libellePeriode(zoom: Zoom, periode: Periode): string {
  if (zoom === 'semaine') return `Semaine du ${format(periode.du, 'd MMM yyyy', { locale: fr })}`
  if (zoom === 'mois') return format(periode.du, 'LLLL yyyy', { locale: fr })
  return `T${format(periode.du, 'Q yyyy')}`
}
