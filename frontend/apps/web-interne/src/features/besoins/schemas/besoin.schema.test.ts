import { addDays } from 'date-fns'
import { describe, expect, it } from 'vitest'
import {
  besoinDefaults,
  besoinSchema,
  justificationRequise,
  rejetSchema,
  toBesoinPayload,
} from './besoin.schema'

const valide = {
  ...besoinDefaults(undefined, 'c0000000-0000-0000-0000-000000000001'),
  categorie: 'MATERIEL_ROULANT' as const,
  designation: 'Pelle hydraulique 20 t',
  quantite: '2',
  dateBesoin: addDays(new Date(), 7),
}

function erreurs(input: unknown) {
  const result = besoinSchema.safeParse(input)
  return result.success ? {} : result.error.flatten().fieldErrors
}

describe('besoinSchema', () => {
  it('accepte un besoin NORMAL sans justification et convertit les nombres', () => {
    const result = besoinSchema.parse(valide)
    expect(result.quantite).toBe(2)
    const payload = toBesoinPayload(result)
    expect(payload.justification).toBeUndefined()
    expect(payload.dateBesoin).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it.each(['URGENT', 'CRITIQUE'] as const)('R08 : exige une justification pour %s', (priorite) => {
    expect(justificationRequise(priorite)).toBe(true)
    expect(erreurs({ ...valide, priorite }).justification?.[0]).toMatch(/obligatoire/)
    expect(erreurs({ ...valide, priorite, justification: 'Arrêt du chantier imminent' })).toEqual({})
  })

  it('refuse une date passée, une quantité nulle et une catégorie absente', () => {
    const fields = erreurs({ ...valide, quantite: 0, categorie: null })
    expect(fields.quantite).toBeDefined()
    expect(fields.categorie).toBeDefined()
    expect(erreurs({ ...valide, dateBesoin: addDays(new Date(), -2) }).dateBesoin).toBeDefined()
  })

  it('exige un motif de rejet', () => {
    expect(rejetSchema.safeParse({ motif: '  ' }).success).toBe(false)
    expect(rejetSchema.safeParse({ motif: 'Doublon du BES-2026-0003' }).success).toBe(true)
  })
})
