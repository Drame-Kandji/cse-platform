import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Role } from '@/shared/domain/labels'
import type { Affectation, MaterielDetail } from '@/shared/domain/types'
import { renderWithProviders, testUser } from '@/test/render'
import { materielPermissions } from '../../lib/permissions'
import { MaterielActions } from './MaterielActions'

const CH1 = 'c0000000-0000-0000-0000-000000000001'
const CH3 = 'c0000000-0000-0000-0000-000000000003'

const enAttente: Affectation = {
  id: 'a1',
  materielId: 'm1',
  materielCode: 'ENG-PEL-01',
  materielDesignation: 'Pelle',
  categorie: 'MATERIEL_ROULANT',
  chantierId: CH1,
  chantierNom: 'Autoroute Dakar-AIBD',
  dateDebut: '2026-10-28',
  dateFinPrevue: '2026-12-15',
  statut: 'EN_ATTENTE_RECEPTION',
}

function materiel(overrides: Partial<MaterielDetail> = {}): MaterielDetail {
  return {
    id: 'm1',
    code: 'ENG-PEL-01',
    designation: 'Pelle hydraulique',
    categorie: 'MATERIEL_ROULANT',
    etat: 'DISPONIBLE',
    heuresFonctionnement: 1200,
    heuresDepuisMaintenance: 100,
    seuilMaintenanceHeures: 500,
    maintenanceDue: false,
    tarifJournalier: 300000,
    reformeDemandee: false,
    version: 2,
    affectations: [],
    maintenances: [],
    pannes: [],
    mouvements: [],
    ...overrides,
  }
}

function renderActions(m: MaterielDetail, roles: Role[], chantierIds: string[] = []) {
  return renderWithProviders(<MaterielActions materiel={m} />, { user: testUser(roles, { chantierIds }) })
}

describe('MaterielActions', () => {
  it('Direction Generale : seule la decision de reforme est proposee (R12, R13)', () => {
    renderActions(materiel({ etat: 'AFFECTE', reformeDemandee: true, motifReforme: 'Vétusté' }), [
      'DIRECTION_GENERALE',
    ])
    expect(screen.getByRole('button', { name: 'Décider de la réforme' })).toBeInTheDocument()
    expect(screen.getAllByRole('button')).toHaveLength(1)
  })

  it('Direction Generale : aucune action sans demande de reforme', () => {
    renderActions(materiel(), ['DIRECTION_GENERALE'])
    expect(screen.queryAllByRole('button')).toHaveLength(0)
  })

  it('Chef de chantier : reception proposee uniquement sur son chantier', () => {
    const m = materiel({ etat: 'AFFECTE', chantierActuelId: CH1, affectations: [enAttente] })
    const { unmount } = renderActions(m, ['CHEF_CHANTIER'], [CH1])
    expect(screen.getByRole('button', { name: 'Confirmer la réception' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Affecter' })).not.toBeInTheDocument()
    unmount()

    renderActions(m, ['CHEF_CHANTIER'], [CH3])
    expect(screen.queryByRole('button', { name: 'Confirmer la réception' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Actions' })).not.toBeInTheDocument()
  })

  it("Responsable Materiel : affecter desactive si le materiel n'est pas DISPONIBLE", async () => {
    renderActions(materiel({ etat: 'EN_MAINTENANCE' }), ['RESPONSABLE_MATERIEL'])
    expect(screen.getByRole('button', { name: 'Affecter' })).toBeDisabled()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Actions' }))
    expect(await screen.findByRole('menuitem', { name: 'Remettre en service' })).toBeInTheDocument()
    expect(screen.getByRole('menuitem', { name: 'Demander la réforme' })).toBeInTheDocument()
    expect(screen.queryByRole('menuitem', { name: 'Libérer' })).not.toBeInTheDocument()
  })
})

describe('materielPermissions', () => {
  const rm = { roles: ['RESPONSABLE_MATERIEL'] as Role[], chantierIds: [] }

  it('un materiel reforme est terminal', () => {
    const can = materielPermissions(materiel({ etat: 'REFORME', reformeDemandee: true }), rm)
    expect(Object.entries(can).filter(([key, value]) => key !== 'affecterBloque' && value)).toEqual([])
    const dg = materielPermissions(materiel({ etat: 'REFORME', reformeDemandee: true }), {
      roles: ['DIRECTION_GENERALE'],
      chantierIds: [],
    })
    expect(dg.validerReforme).toBe(false)
  })

  it('RM : liberation si affecte, pas de nouvelle demande de reforme si deja demandee', () => {
    const can = materielPermissions(materiel({ etat: 'AFFECTE', reformeDemandee: true }), rm)
    expect(can).toMatchObject({ liberer: true, demanderReforme: false, changerEtat: true, affecter: true })
    expect(can.affecterBloque).not.toBeNull()
  })

  it('Chef : releve et panne sur son chantier courant uniquement', () => {
    const m = materiel({ etat: 'AFFECTE', chantierActuelId: CH1 })
    expect(materielPermissions(m, { roles: ['CHEF_CHANTIER'], chantierIds: [CH1] })).toMatchObject({
      releverHeures: true,
      signalerPanne: true,
      liberer: false,
    })
    expect(materielPermissions(m, { roles: ['CHEF_CHANTIER'], chantierIds: [CH3] })).toMatchObject({
      releverHeures: false,
      signalerPanne: false,
    })
  })
})
