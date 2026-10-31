import { format, formatDistanceToNow, isValid, parseISO } from 'date-fns'
import { fr } from 'date-fns/locale'

/** Locale utilisee pour tous les formats numeriques (Senegal, francais). */
export const LOCALE = 'fr-SN'
/** Espace insecable entre valeur et unite. */
const NBSP = '\u00a0'
/** Valeur affichee lorsqu'une donnee est absente. */
export const EMPTY_VALUE = '—'

type Nullable<T> = T | null | undefined
type DateInput = Nullable<Date | string | number>

const integerFormat = new Intl.NumberFormat(LOCALE, { maximumFractionDigits: 0 })
const compactFormat = new Intl.NumberFormat(LOCALE, { notation: 'compact', maximumFractionDigits: 1 })

function isFiniteNumber(value: Nullable<number>): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

/**
 * Formate un montant en francs CFA, sans decimales.
 * @example formatMoney(1250000) // "1 250 000 FCFA"
 * @param options.compact affiche "1,3 M FCFA" pour les tableaux de bord
 */
export function formatMoney(value: Nullable<number>, options: { compact?: boolean } = {}): string {
  if (!isFiniteNumber(value)) return EMPTY_VALUE
  const formatted = options.compact ? compactFormat.format(value) : integerFormat.format(Math.round(value))
  return `${formatted}${NBSP}FCFA`
}

/**
 * Formate un nombre avec separateurs de milliers francais.
 * @param options.decimals nombre maximal de decimales (0 par defaut)
 */
export function formatNumber(
  value: Nullable<number>,
  options: { decimals?: number; compact?: boolean } = {},
): string {
  if (!isFiniteNumber(value)) return EMPTY_VALUE
  if (options.compact) return compactFormat.format(value)
  return new Intl.NumberFormat(LOCALE, { maximumFractionDigits: options.decimals ?? 0 }).format(value)
}

/**
 * Formate un ratio en pourcentage.
 * @param value ratio entre 0 et 1 (0.125 => "12,5 %")
 */
export function formatPercent(
  value: Nullable<number>,
  options: { decimals?: number; signed?: boolean } = {},
): string {
  if (!isFiniteNumber(value)) return EMPTY_VALUE
  return new Intl.NumberFormat(LOCALE, {
    style: 'percent',
    maximumFractionDigits: options.decimals ?? 1,
    signDisplay: options.signed ? 'exceptZero' : 'auto',
  }).format(value)
}

/** Convertit une entree (ISO, timestamp, Date) en Date valide ou null. */
export function toDate(value: DateInput): Date | null {
  if (value === null || value === undefined || value === '') return null
  const date = typeof value === 'string' ? parseISO(value) : new Date(value)
  return isValid(date) ? date : null
}

/** Date courte francaise : "12 mars 2026". */
export function formatDate(value: DateInput, pattern = 'd MMM yyyy'): string {
  const date = toDate(value)
  return date ? format(date, pattern, { locale: fr }) : EMPTY_VALUE
}

/** Date numerique : "12/03/2026". */
export function formatDateShort(value: DateInput): string {
  return formatDate(value, 'dd/MM/yyyy')
}

/** Date et heure : "12 mars 2026 a 14:30". */
export function formatDateTime(value: DateInput): string {
  return formatDate(value, "d MMM yyyy 'à' HH:mm")
}

/** Duree relative : "il y a 3 heures". */
export function formatRelative(value: DateInput): string {
  const date = toDate(value)
  return date ? formatDistanceToNow(date, { addSuffix: true, locale: fr }) : EMPTY_VALUE
}

/** Serialise une date au format API "yyyy-MM-dd" (LocalDate Java). */
export function toIsoDate(value: Date | null | undefined): string | undefined {
  return value && isValid(value) ? format(value, 'yyyy-MM-dd') : undefined
}

/** Taille de fichier lisible : "1,2 Mo". */
export function formatFileSize(bytes: Nullable<number>): string {
  if (!isFiniteNumber(bytes)) return EMPTY_VALUE
  const units = ['o', 'Ko', 'Mo', 'Go']
  let size = bytes
  let unit = 0
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024
    unit += 1
  }
  return `${formatNumber(size, { decimals: unit === 0 ? 0 : 1 })}${NBSP}${units[unit] ?? 'o'}`
}
