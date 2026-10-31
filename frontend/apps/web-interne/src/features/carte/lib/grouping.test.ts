import { describe, expect, it } from 'vitest'
import type { CarteFeature, CarteFeatureProperties } from '@/shared/domain/types'
import { effectifGroupe, etatDominant, grouperParPosition, tailleMarqueur } from './grouping'

function feature(lat: number, lng: number, properties: Partial<CarteFeatureProperties>): CarteFeature {
  return {
    type: 'Feature',
    geometry: { type: 'Point', coordinates: [lng, lat] },
    properties: { kind: 'MATERIEL', id: 'x', code: 'X', libelle: 'X', ...properties },
  }
}

// Chantiers de demonstration (API_CONTRACT §12).
const DAKAR = { lat: 14.7167, lng: -17.4677 }
const THIES = { lat: 14.791, lng: -16.9256 }

describe('grouperParPosition', () => {
  it('rattache les engins a leur chantier par chantierNom malgre leur position GPS propre', () => {
    const groupes = grouperParPosition([
      feature(DAKAR.lat, DAKAR.lng, {
        kind: 'CHANTIER',
        id: 'c1',
        code: 'CH-DKR-01',
        libelle: 'Autoroute Dakar-AIBD',
        nombreMateriels: 3,
      }),
      // Decalages du jeu de donnees dev (+/- 0,003 degre autour du chantier).
      feature(DAKAR.lat - 0.003, DAKAR.lng - 0.0018, {
        id: 'm1',
        etat: 'AFFECTE',
        chantierNom: 'Autoroute Dakar-AIBD',
      }),
      feature(DAKAR.lat + 0.0015, DAKAR.lng, {
        id: 'm2',
        etat: 'AFFECTE',
        chantierNom: 'Autoroute Dakar-AIBD',
      }),
      feature(DAKAR.lat, DAKAR.lng + 0.0018, {
        id: 'm3',
        etat: 'EN_MAINTENANCE',
        chantierNom: 'Autoroute Dakar-AIBD',
      }),
      feature(THIES.lat, THIES.lng, {
        kind: 'CHANTIER',
        id: 'c2',
        code: 'CH-THS-01',
        libelle: 'Contournement de Thiès',
      }),
    ])
    expect(groupes).toHaveLength(2)
    const [dakar, thies] = groupes
    expect(dakar?.chantier?.id).toBe('c1')
    expect(dakar?.lat).toBe(DAKAR.lat)
    expect(dakar?.lng).toBe(DAKAR.lng)
    expect(dakar?.materiels.map((m) => m.id)).toEqual(['m1', 'm2', 'm3'])
    expect(dakar?.parEtat).toEqual({ DISPONIBLE: 0, AFFECTE: 2, EN_MAINTENANCE: 1, REFORME: 0 })
    expect(thies?.materiels).toHaveLength(0)
  })

  it('rattache par chantierId en priorite (chantiers homonymes, nom absent)', () => {
    const groupes = grouperParPosition([
      feature(DAKAR.lat, DAKAR.lng, { kind: 'CHANTIER', id: 'c1', libelle: 'Chantier A' }),
      feature(THIES.lat, THIES.lng, { kind: 'CHANTIER', id: 'c2', libelle: 'Chantier A' }),
      feature(THIES.lat + 0.002, THIES.lng, { id: 'm1', chantierId: 'c2', chantierNom: 'Chantier A' }),
      feature(DAKAR.lat + 0.002, DAKAR.lng, { id: 'm2', chantierId: 'c1' }),
    ])
    expect(groupes.find((g) => g.chantier?.id === 'c1')?.materiels.map((m) => m.id)).toEqual(['m2'])
    expect(groupes.find((g) => g.chantier?.id === 'c2')?.materiels.map((m) => m.id)).toEqual(['m1'])
  })

  it('isole les materiels hors chantier et place les chantiers en premier', () => {
    const groupes = grouperParPosition([
      feature(14.69, -17.44, { id: 'depot-1', libelle: 'Compresseur', etat: 'DISPONIBLE' }),
      feature(14.69, -17.44, { id: 'depot-2', libelle: 'Groupe', etat: 'DISPONIBLE' }),
      feature(16.0326, -16.4818, { kind: 'CHANTIER', id: 'c3', libelle: 'Pont de la lagune' }),
    ])
    expect(groupes.map((g) => g.chantier?.id ?? 'depot')).toEqual(['c3', 'depot'])
    expect(groupes[1]?.materiels).toHaveLength(2)
    expect(groupes[1] && etatDominant(groupes[1])).toBe('DISPONIBLE')
  })

  it('garde a sa position un engin dont le chantier est absent de la reponse', () => {
    const groupes = grouperParPosition([
      feature(12.5681, -16.2719, { id: 'm9', chantierNom: 'Bâtiment administratif', etat: 'AFFECTE' }),
    ])
    expect(groupes).toHaveLength(1)
    expect(groupes[0]?.chantier).toBeNull()
    expect(groupes[0]?.lat).toBeCloseTo(12.5681)
  })

  it('ignore les coordonnees invalides', () => {
    expect(grouperParPosition([feature(Number.NaN, 0, {}), feature(120, 0, {})])).toEqual([])
  })

  it("derive l'effectif et la taille des marqueurs", () => {
    const [groupe] = grouperParPosition([
      feature(1, 1, { kind: 'CHANTIER', nombreMateriels: 9 }),
      feature(1, 1, { id: 'm' }),
    ])
    expect(groupe && effectifGroupe(groupe)).toBe(9)
    expect(tailleMarqueur(0)).toBe(28)
    expect(tailleMarqueur(100)).toBe(56)
    expect(tailleMarqueur(9)).toBeGreaterThan(tailleMarqueur(1))
  })
})
