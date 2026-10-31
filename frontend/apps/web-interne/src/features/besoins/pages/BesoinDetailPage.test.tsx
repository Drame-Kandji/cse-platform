import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { Route, Routes } from 'react-router-dom'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { renderWithProviders, testUser } from '@/test/render'
import type { Besoin } from '../api/types'
import { api, besoinFixture } from '../testing/fixtures'
import { Component as BesoinDetailPage } from './BesoinDetailPage'

const ID = 'b0000000-0000-0000-0000-000000000001'
const CHEF_ID = '00000000-0000-0000-0000-000000000101'
let rejet: unknown = null

function handlers(besoin: Besoin) {
  return [
    http.get(api(`/besoins/${ID}`), () => HttpResponse.json(besoin)),
    http.get(api(`/besoins/${ID}/historique`), () =>
      HttpResponse.json([
        {
          id: 'h1',
          statut: 'EN_ATTENTE',
          action: 'Besoin créé',
          auteurId: CHEF_ID,
          auteurNom: 'Mamadou Diop',
          date: '2026-10-28T09:15:00Z',
        },
      ]),
    ),
    http.patch(api(`/besoins/${ID}/rejeter`), async ({ request }) => {
      rejet = await request.json()
      return HttpResponse.json({ ...besoin, statut: 'REJETE', motifRejet: 'Doublon du BES-2026-0003' })
    }),
  ]
}

const server = setupServer()
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => {
  server.resetHandlers()
  rejet = null
})
afterAll(() => {
  server.close()
})

function renderDetail(roles: Parameters<typeof testUser>[0], id = 'u-1') {
  return renderWithProviders(
    <Routes>
      <Route path="/besoins/:id" element={<BesoinDetailPage />} />
    </Routes>,
    { user: testUser(roles, { id }), route: `/besoins/${ID}` },
  )
}

describe('BesoinDetailPage', () => {
  it('masque valider / rejeter pour un chef et lui propose de modifier son besoin en attente', async () => {
    server.use(...handlers(besoinFixture()))
    renderDetail(['CHEF_CHANTIER'], CHEF_ID)
    expect(await screen.findByRole('heading', { name: 'Pelle hydraulique 20 t' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Modifier/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Valider' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rejeter' })).not.toBeInTheDocument()
    expect(await screen.findByText('Besoin créé')).toBeInTheDocument()
  })

  it('ne propose aucune action sur un besoin déjà traité et affiche le motif de rejet', async () => {
    server.use(...handlers(besoinFixture({ statut: 'REJETE', motifRejet: 'Matériel disponible à Thiès' })))
    renderDetail(['RESPONSABLE_MATERIEL'])
    expect(await screen.findByText('Matériel disponible à Thiès')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Valider' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Modifier/ })).not.toBeInTheDocument()
  })

  it('exige un motif avant de rejeter (RM)', async () => {
    server.use(...handlers(besoinFixture()))
    const user = userEvent.setup()
    renderDetail(['RESPONSABLE_MATERIEL'])
    expect(await screen.findByRole('button', { name: 'Valider' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Modifier/ })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Rejeter' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Rejeter le besoin' }))
    expect(await within(dialog).findByText(/Motif obligatoire/)).toBeInTheDocument()
    expect(rejet).toBeNull()

    await user.type(within(dialog).getByLabelText(/Motif du rejet/), 'Doublon du BES-2026-0003')
    await user.click(within(dialog).getByRole('button', { name: 'Rejeter le besoin' }))
    await waitFor(() => {
      expect(rejet).toEqual({ motif: 'Doublon du BES-2026-0003' })
    })
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })
})
