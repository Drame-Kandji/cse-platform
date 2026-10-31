import { describe, expect, it } from 'vitest'
import type { SectionParc } from '../types'
import { disponibiliteHint } from './kpis'

const parc: SectionParc = {
  total: 42,
  disponibles: 11,
  affectes: 28,
  enMaintenance: 2,
  reformes: 1,
  tauxDisponibilite: 95.1,
  tauxUtilisation: 70,
  maintenancesEnCours: 2,
  pannesOuvertes: 1,
  alertesMaintenance: 3,
  parCategorie: [],
}

describe('disponibiliteHint', () => {
  it('affiche les materiels operationnels sur le parc hors reformes', () => {
    expect(disponibiliteHint({ ...parc, operationnels: 39 })).toBe('39 opérationnels sur 41')
  })

  it("se replie sur les disponibles tant que l'API ne fournit pas operationnels", () => {
    expect(disponibiliteHint(parc)).toBe('11 disponibles sur 41')
    expect(disponibiliteHint({ ...parc, operationnels: null })).toBe('11 disponibles sur 41')
  })
})
