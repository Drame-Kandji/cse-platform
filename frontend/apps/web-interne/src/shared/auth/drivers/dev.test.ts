import { describe, expect, it } from 'vitest'
import { DEV_PERSONAS, personaUser } from './dev'

describe('profils du mode dev', () => {
  it('reprennent les comptes de demonstration partages (contrat section 12)', () => {
    expect(DEV_PERSONAS.RESPONSABLE_ACHATS).toMatchObject({ name: 'Fatou Sarr', email: 'resp.achats@cse.sn' })
    expect(DEV_PERSONAS.RESPONSABLE_MATERIEL).toMatchObject({
      name: 'Ibrahima Fall',
      email: 'resp.materiel@cse.sn',
    })
    expect(DEV_PERSONAS.CHEF_CHANTIER.name).toBe('Mamadou Diop')
    expect(DEV_PERSONAS.DAF.name).toBe('Ousmane Ba')
    expect(DEV_PERSONAS.DIRECTION_GENERALE.name).toBe('Aminata Sow')
    expect(DEV_PERSONAS.ADMIN_SYSTEME.name).toBe('Cheikh Ndiaye')
  })

  it('rattache le chef de chantier aux chantiers 1 et 2 du jeu de donnees', () => {
    expect(personaUser('CHEF_CHANTIER')).toMatchObject({
      id: '00000000-0000-0000-0000-000000000101',
      roles: ['CHEF_CHANTIER'],
      chantierIds: ['c0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002'],
    })
  })
})
