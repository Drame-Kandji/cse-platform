import { describe, expect, it } from 'vitest'
import {
  EMPTY_VALUE,
  formatDate,
  formatDateShort,
  formatDateTime,
  formatFileSize,
  formatMoney,
  formatNumber,
  formatPercent,
  sansTiretsLongs,
  toIsoDate,
} from './format'
import { initials } from './text'

/** Normalise les espaces insecables (Intl en produit plusieurs variantes). */
const plain = (value: string) => value.replace(/[\u00a0\u202f]/g, ' ')

describe('format', () => {
  it('formate les montants en FCFA sans decimales', () => {
    expect(plain(formatMoney(1250000))).toBe('1 250 000 FCFA')
    expect(plain(formatMoney(1999.6))).toBe('2 000 FCFA')
    expect(plain(formatMoney(0))).toBe('0 FCFA')
    expect(formatMoney(null)).toBe(EMPTY_VALUE)
    expect(formatMoney(Number.NaN)).toBe(EMPTY_VALUE)
  })
  it('formate les montants compacts', () => {
    expect(plain(formatMoney(3_480_000_000, { compact: true }))).toMatch(/^3,5 Md FCFA$/)
  })
  it('formate nombres et pourcentages a la francaise', () => {
    expect(plain(formatNumber(12345.678, { decimals: 2 }))).toBe('12 345,68')
    expect(plain(formatPercent(0.125))).toBe('12,5 %')
    expect(plain(formatPercent(0.04, { signed: true }))).toBe('+4 %')
  })
  it('formate les dates en francais', () => {
    expect(formatDate('2026-03-12')).toBe('12 mars 2026')
    expect(formatDateShort('2026-03-12T10:00:00')).toBe('12/03/2026')
    expect(formatDateTime('2026-03-12T14:30:00')).toBe('12 mars 2026 à 14:30')
    expect(formatDate('pas une date')).toBe(EMPTY_VALUE)
    expect(formatDate(undefined)).toBe(EMPTY_VALUE)
  })
  it('serialise une date au format API', () => {
    expect(toIsoDate(new Date(2026, 0, 5))).toBe('2026-01-05')
    expect(toIsoDate(null)).toBeUndefined()
  })
  it('formate tailles et initiales', () => {
    expect(plain(formatFileSize(2_400_000))).toBe('2,3 Mo')
    expect(initials('Moussa Ndiaye')).toBe('MN')
    expect(initials('awa')).toBe('A')
    expect(initials('')).toBe('?')
  })
})

describe('sansTiretsLongs', () => {
  it('remplace les tirets longs par un point median ou un trait d union', () => {
    expect(sansTiretsLongs('Matériel A — affecté')).toBe('Matériel A · affecté')
    expect(sansTiretsLongs('Route Kaolack–Fatick')).toBe('Route Kaolack-Fatick')
    expect(sansTiretsLongs('—')).toBe('-')
  })
})
