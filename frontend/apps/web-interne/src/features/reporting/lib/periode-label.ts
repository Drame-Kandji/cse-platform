import { format, isValid, parseISO } from 'date-fns'
import { fr } from 'date-fns/locale'

/**
 * Libelle court d'une periode de serie : "2026-07" -> "juil. 26", "2026-07-14" -> "14 juil.",
 * "2026-W29" -> "S29". Valeur inconnue rendue telle quelle.
 */
export function periodeLabel(periode: string): string {
  const week = /^(\d{4})-W(\d{1,2})$/.exec(periode)
  if (week) return `S${week[2] ?? ''}`
  if (/^\d{4}-\d{2}$/.test(periode)) {
    const date = parseISO(`${periode}-01`)
    return isValid(date) ? format(date, 'MMM yy', { locale: fr }) : periode
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(periode)) {
    const date = parseISO(periode)
    return isValid(date) ? format(date, 'd MMM', { locale: fr }) : periode
  }
  return periode
}
