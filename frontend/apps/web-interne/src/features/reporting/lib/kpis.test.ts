import { describe, expect, it } from 'vitest'
import type { SectionParc, Tableau } from '../types'
import { dashboardKpis, disponibiliteHint } from './kpis'

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

describe('dashboardKpis', () => {
  it('retient les 4 indicateurs prioritaires du profil', () => {
    const tableau: Tableau = {
      profil: 'RESPONSABLE_MATERIEL',
      du: '2026-08-01',
      au: '2026-10-30',
      genereLe: '2026-10-30T08:00:00Z',
      parc: { ...parc, operationnels: 39 },
      besoins: {
        total: 10,
        enAttente: 2,
        valides: 5,
        rejetes: 1,
        affectesDirect: 1,
        enAchat: 1,
        delaiMoyenTraitementHeures: null,
        parPriorite: { NORMAL: 8, URGENT: 1, CRITIQUE: 1 },
      },
    }
    expect(dashboardKpis(tableau).map((kpi) => kpi.id)).toEqual([
      'disponibilite',
      'utilisation',
      'pannes',
      'besoins',
    ])
  })
})
