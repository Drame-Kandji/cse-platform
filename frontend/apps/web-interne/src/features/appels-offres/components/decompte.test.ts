import { describe, expect, it } from 'vitest'
import { decompte } from './decompte'
import { contribution, nomOffre } from './offre-display'

const NOW = Date.parse('2026-10-30T12:00:00Z')

describe('decompte', () => {
  it('formate le temps restant avec un ton d’urgence', () => {
    expect(decompte('2026-11-05T15:00:00Z', NOW)).toEqual({
      label: 'Reste J-6 · 3 h',
      tone: 'info',
      expire: false,
    })
    expect(decompte('2026-11-01T12:00:00Z', NOW).tone).toBe('warning')
    expect(decompte('2026-10-30T14:05:00Z', NOW)).toEqual({
      label: 'Reste 2 h 05 min',
      tone: 'danger',
      expire: false,
    })
    expect(decompte('2026-10-30T11:00:00Z', NOW)).toEqual({
      label: 'Échéance dépassée',
      tone: 'neutral',
      expire: true,
    })
  })
})

describe('offre-display', () => {
  it('anonymise tant que PUBLIE (R10)', () => {
    const o = { codeAnonyme: 'OFF-A1', fournisseurNom: 'Sahel Engins SA' }
    expect(nomOffre(o, 'PUBLIE')).toBe('OFF-A1')
    expect(nomOffre(o, 'CLOTURE')).toBe('Sahel Engins SA (OFF-A1)')
  })
  it('calcule la contribution ponderee', () => {
    expect(contribution(80, 40)).toBe(32)
    expect(contribution(undefined, 40)).toBeUndefined()
  })
})
