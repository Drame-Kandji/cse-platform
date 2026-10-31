import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import type { Chantier } from '@/shared/domain/types'
import { renderWithProviders, testUser } from '@/test/render'
import { Component as ChantiersPage } from './ChantiersPage'

const api = (path: string) => `${window.location.origin}/api${path}`
const page = <T,>(content: T[]) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
})
const chantier: Chantier = {
  id: 'c0000000-0000-0000-0000-000000000001',
  code: 'CH-DKR-01',
  nom: 'Extension autoroute Dakar-AIBD',
  localisation: 'Diamniadio',
  region: 'Dakar',
  latitude: 14.7167,
  longitude: -17.4677,
  statut: 'EN_COURS',
  budget: 4_500_000_000,
  montantMarche: 5_200_000_000,
  dateDebut: '2026-02-01',
  dateFinPrevue: '2027-06-30',
  chefChantierNom: 'Mamadou Diop',
  nombreMateriels: 12,
  version: 1,
}
const server = setupServer(
  http.get(api('/chantiers'), () => HttpResponse.json(page([chantier]))),
  http.get(api(`/chantiers/${chantier.id}`), () => HttpResponse.json(chantier)),
  http.get(api('/affectations'), () => HttpResponse.json(page([]))),
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

describe('ChantiersPage', () => {
  it('affiche les chantiers et les actions de gestion au RM', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ChantiersPage />, { user: testUser(['RESPONSABLE_MATERIEL']), route: '/chantiers' })
    expect(await screen.findByText('Extension autoroute Dakar-AIBD')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Nouveau chantier/ })).toBeInTheDocument()

    // Vue tableau par defaut : un clic sur la ligne ouvre le detail.
    await user.click(screen.getByText('Extension autoroute Dakar-AIBD'))
    const sheet = await screen.findByRole('dialog')
    expect(await within(sheet).findByText(/Aucun matériel n'est affecté/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Terminer le chantier' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Modifier' })).toBeInTheDocument()
  })

  it('reste en lecture seule pour un chef de chantier', async () => {
    renderWithProviders(<ChantiersPage />, { user: testUser(['CHEF_CHANTIER']), route: '/chantiers' })
    expect(await screen.findByText('Extension autoroute Dakar-AIBD')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Nouveau chantier/ })).not.toBeInTheDocument()
  })
})
