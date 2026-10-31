import { describe, expect, it } from 'vitest'
import { trierParPrioriteEtAnciennete } from './priorite'

describe('trierParPrioriteEtAnciennete', () => {
  it('place les critiques en tête puis les plus anciens', () => {
    const ordre = trierParPrioriteEtAnciennete([
      { id: 'a', priorite: 'NORMAL', createdAt: '2026-10-01T08:00:00Z' },
      { id: 'b', priorite: 'CRITIQUE', createdAt: '2026-10-20T08:00:00Z' },
      { id: 'c', priorite: 'URGENT', createdAt: '2026-10-05T08:00:00Z' },
      { id: 'd', priorite: 'CRITIQUE', createdAt: '2026-10-10T08:00:00Z' },
    ] as const)
    expect(ordre.map((b) => b.id)).toEqual(['d', 'b', 'c', 'a'])
  })
})
