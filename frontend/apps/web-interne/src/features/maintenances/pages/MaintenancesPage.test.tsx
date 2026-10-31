import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import type { Maintenance } from '@/shared/domain/types'
import { renderWithProviders, testUser } from '@/test/render'
import { Component as MaintenancesPage } from './MaintenancesPage'

const API = `${window.location.origin}/api`
const ot: Maintenance = {
  id: 'm1',
  numeroOt: 'OT-2026-0012',
  materielId: 'mat1',
  materielCode: 'PEL-001',
  materielDesignation: 'Pelle hydraulique',
  type: 'PREVENTIVE',
  statut: 'PLANIFIEE',
  description: 'Vidange 500 h',
  datePlanifiee: '2026-11-04',
}
const pageOf = (content: unknown[]) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
})
const server = setupServer(
  http.get(`${API}/maintenances`, () => HttpResponse.json(pageOf([ot]))),
  http.get(`${API}/pannes`, () => HttpResponse.json(pageOf([]))),
)
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => {
  server.resetHandlers()
})
afterAll(() => {
  server.close()
})

describe('MaintenancesPage', () => {
  it('propose planifier et démarrer au Responsable Matériel', async () => {
    renderWithProviders(<MaintenancesPage />, {
      user: testUser(['RESPONSABLE_MATERIEL']),
      route: '/maintenances',
    })
    expect(await screen.findByText('OT-2026-0012')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Démarrer/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Planifier une maintenance/ })).toBeInTheDocument()
  })

  it('limite le chef de chantier au signalement de panne', async () => {
    renderWithProviders(<MaintenancesPage />, { user: testUser(['CHEF_CHANTIER']), route: '/maintenances' })
    expect(await screen.findByText('OT-2026-0012')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Démarrer/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Planifier une maintenance/ })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Signaler une panne/ })).toBeInTheDocument()
  })
})
