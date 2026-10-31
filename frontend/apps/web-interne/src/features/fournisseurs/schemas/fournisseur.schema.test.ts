import { describe, expect, it } from 'vitest'
import { fournisseurSchema } from './fournisseur.schema'

const valide = {
  raisonSociale: 'Sahel Engins SA',
  ninea: '005123456 2G3',
  pays: 'Sénégal',
  email: 'contact@sahel-engins.sn',
  telephone: '',
  specialites: ['MATERIEL_ROULANT'],
  certifications: '',
  banque: '',
  iban: '',
}

describe('fournisseurSchema', () => {
  it('accepte un fournisseur conforme aux regles du backend', () => {
    expect(fournisseurSchema.safeParse(valide).success).toBe(true)
  })
  it('refuse un NINEA, un telephone ou un IBAN mal formes', () => {
    expect(fournisseurSchema.safeParse({ ...valide, ninea: '12345' }).success).toBe(false)
    expect(fournisseurSchema.safeParse({ ...valide, telephone: 'abc' }).success).toBe(false)
    expect(fournisseurSchema.safeParse({ ...valide, banque: 'CBAO', iban: 'FAUX' }).success).toBe(false)
  })
  it('exige la banque et l IBAN ensemble', () => {
    expect(fournisseurSchema.safeParse({ ...valide, banque: 'CBAO' }).success).toBe(false)
  })
})
