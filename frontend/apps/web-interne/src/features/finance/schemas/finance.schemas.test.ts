import { describe, expect, it } from 'vitest'
import { decisionEngagementSchema, factureSchema, mandatSchema } from './finance.schemas'

describe('schemas finance', () => {
  it('refuse une reference de mandat vide', () => {
    expect(mandatSchema.safeParse({ referenceMandat: '   ' }).success).toBe(false)
    expect(mandatSchema.parse({ referenceMandat: ' MDT-1 ' })).toEqual({ referenceMandat: 'MDT-1' })
  })
  it('exige un commentaire en cas de refus seulement', () => {
    expect(decisionEngagementSchema.safeParse({ approuve: false, commentaire: '' }).success).toBe(false)
    expect(decisionEngagementSchema.safeParse({ approuve: true, commentaire: '' }).success).toBe(true)
  })
  it("convertit les montants et controle l'echeance", () => {
    const base = {
      contratId: 'c1',
      numero: 'FAC-1',
      montantHt: '1000',
      tva: '180',
      dateEmission: new Date(2026, 9, 1),
      dateEcheance: new Date(2026, 10, 1),
    }
    expect(factureSchema.parse(base)).toMatchObject({ montantHt: 1000, tva: 180 })
    expect(factureSchema.safeParse({ ...base, montantHt: '12,5' }).success).toBe(false)
    expect(factureSchema.safeParse({ ...base, dateEcheance: new Date(2026, 8, 1) }).success).toBe(false)
  })
})
