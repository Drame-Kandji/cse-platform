import { describe, expect, it } from 'vitest'
import type { Affectation } from '@/shared/domain/types'
import {
  decaler,
  graduations,
  jourAuPoint,
  libellePeriode,
  nombreDeJours,
  periodeDe,
  positionBarre,
  positionJour,
  type Periode,
} from './layout'
import { finEffective, grouperPlanning } from './rows'

const d = (iso: string) => new Date(`${iso}T00:00:00`)
/** Octobre 2026 : 31 jours. */
const OCTOBRE: Periode = { du: d('2026-10-01'), au: d('2026-10-31') }

describe('periodes', () => {
  it('calcule semaine (lundi-dimanche), mois et trimestre contenant la date', () => {
    const ancre = d('2026-10-30') // vendredi
    expect(periodeDe('semaine', ancre)).toEqual({ du: d('2026-10-26'), au: d('2026-11-01') })
    expect(periodeDe('mois', ancre)).toEqual(OCTOBRE)
    expect(periodeDe('trimestre', ancre)).toEqual({ du: d('2026-10-01'), au: d('2026-12-31') })
    expect(nombreDeJours(periodeDe('trimestre', ancre))).toBe(92)
  })

  it('navigue vers la periode precedente ou suivante', () => {
    expect(periodeDe('mois', decaler('mois', d('2026-10-30'), -1)).du).toEqual(d('2026-09-01'))
    expect(periodeDe('semaine', decaler('semaine', d('2026-10-30'), 1)).du).toEqual(d('2026-11-02'))
    expect(periodeDe('trimestre', decaler('trimestre', d('2026-10-30'), 1)).du).toEqual(d('2027-01-01'))
  })

  it('libelle la periode en francais', () => {
    expect(libellePeriode('mois', OCTOBRE)).toBe('octobre 2026')
    expect(libellePeriode('trimestre', periodeDe('trimestre', d('2026-10-30')))).toBe('T4 2026')
    expect(libellePeriode('semaine', periodeDe('semaine', d('2026-10-30')))).toBe('Semaine du 26 oct. 2026')
  })
})

describe('positionBarre', () => {
  it('positionne une affectation entierement incluse (jours inclus)', () => {
    const position = positionBarre('2026-10-05', '2026-10-11', OCTOBRE)
    expect(position?.left).toBeCloseTo((4 / 31) * 100)
    expect(position?.width).toBeCloseTo((7 / 31) * 100)
    expect(position).toMatchObject({ coupeeAvant: false, coupeeApres: false })
  })

  it('coupe une affectation qui deborde de la periode', () => {
    const position = positionBarre('2026-09-20', '2026-11-15', OCTOBRE)
    expect(position).toMatchObject({ left: 0, coupeeAvant: true, coupeeApres: true })
    expect(position?.width).toBeCloseTo(100)
    const fin = positionBarre('2026-10-30', '2026-11-04', OCTOBRE)
    expect(fin?.left).toBeCloseTo((29 / 31) * 100)
    expect(fin?.width).toBeCloseTo((2 / 31) * 100)
    expect(fin?.coupeeApres).toBe(true)
  })

  it("ignore une affectation hors periode ou d'un seul jour mal formee", () => {
    expect(positionBarre('2026-11-02', '2026-11-10', OCTOBRE)).toBeNull()
    expect(positionBarre('2026-09-01', '2026-09-30', OCTOBRE)).toBeNull()
    expect(positionBarre('2026-10-10', '2026-10-05', OCTOBRE)).toBeNull()
    expect(positionBarre('pas-une-date', '2026-10-05', OCTOBRE)).toBeNull()
    expect(positionBarre('2026-10-31', '2026-10-31', OCTOBRE)?.width).toBeCloseTo(100 / 31)
  })
})

describe('reperes', () => {
  it('place le repere du jour au milieu de la journee, null hors periode', () => {
    expect(positionJour(d('2026-10-01'), OCTOBRE)).toBeCloseTo((0.5 / 31) * 100)
    expect(positionJour(d('2026-11-01'), OCTOBRE)).toBeNull()
  })

  it('convertit une abscisse en jour (borne a la periode)', () => {
    expect(jourAuPoint(0, OCTOBRE)).toEqual(d('2026-10-01'))
    expect(jourAuPoint(0.5, OCTOBRE)).toEqual(d('2026-10-16'))
    expect(jourAuPoint(1.2, OCTOBRE)).toEqual(d('2026-10-31'))
  })

  it('produit des graduations journalieres (mois) ou hebdomadaires (trimestre)', () => {
    const jours = graduations('mois', OCTOBRE)
    expect(jours).toHaveLength(31)
    expect(jours[2]).toMatchObject({ label: '3', weekend: true }) // samedi 3 octobre
    const semaines = graduations('trimestre', periodeDe('trimestre', d('2026-10-30')))
    expect(semaines[0]).toMatchObject({ left: 0, sousLabel: 'S40' })
    const total = semaines.reduce((sum, tick) => sum + tick.width, 0)
    expect(total).toBeCloseTo(100)
  })
})

function affectation(overrides: Partial<Affectation>): Affectation {
  return {
    id: 'a',
    materielId: 'm1',
    materielCode: 'ENG-01',
    materielDesignation: 'Pelle',
    categorie: 'MATERIEL_ROULANT',
    chantierId: 'c1',
    chantierNom: 'Thiès',
    dateDebut: '2026-10-01',
    dateFinPrevue: '2026-10-10',
    statut: 'EN_COURS',
    ...overrides,
  }
}

describe('grouperPlanning', () => {
  it('regroupe par chantier puis materiel, trie et ordonne les affectations', () => {
    const groupes = grouperPlanning([
      affectation({ id: 'a2', dateDebut: '2026-10-20', dateFinPrevue: '2026-10-25' }),
      affectation({ id: 'a1' }),
      affectation({ id: 'a3', materielId: 'm0', materielCode: 'AAA-01' }),
      affectation({ id: 'a4', chantierId: 'c0', chantierNom: 'Dakar' }),
    ])
    expect(groupes.map((g) => g.chantierNom)).toEqual(['Dakar', 'Thiès'])
    const thies = groupes[1]
    expect(thies?.lignes.map((l) => l.materielCode)).toEqual(['AAA-01', 'ENG-01'])
    expect(thies?.lignes[1]?.affectations.map((a) => a.id)).toEqual(['a1', 'a2'])
  })

  it('utilise la fin reelle quand elle est connue', () => {
    expect(finEffective({ dateFinPrevue: '2026-10-10', dateFinReelle: '2026-10-08' })).toBe('2026-10-08')
    expect(finEffective({ dateFinPrevue: '2026-10-10' })).toBe('2026-10-10')
  })
})
