import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { configureHttp } from '@/shared/api'
import { renderWithProviders, testUser } from '@/test/render'
import type { Engagement, Facture, SyntheseFinance } from '../api/types'
import { Component as FinancePage } from './FinancePage'

const API = `${window.location.origin}/api`

const synthese: SyntheseFinance = {
  montantEngage: 250_000_000,
  montantEngagementsEnAttente: 45_000_000,
  montantFacture: 180_000_000,
  montantMandate: 120_000_000,
  montantPaye: 90_000_000,
  facturesEnRetard: 1,
  engagementsEnAttente: 1,
}
const engagement: Engagement = {
  contratId: 'ctr-1',
  contratNumero: 'CTR-2026-001',
  fournisseurNom: 'Sahel Engins SA',
  chantierNom: 'Extension autoroute Dakar–AIBD',
  objet: 'Pelle hydraulique 20 t',
  montant: 45_000_000,
  statut: 'EN_ATTENTE',
  dateDemande: '2026-10-20T09:00:00Z',
}
const facture: Facture = {
  id: 'fac-1',
  numero: 'FAC-2026-0042',
  contratId: 'ctr-1',
  contratNumero: 'CTR-2026-001',
  fournisseurId: 'f0000000-0000-0000-0000-000000000001',
  fournisseurNom: 'Sahel Engins SA',
  montantHt: 10_000_000,
  tva: 1_800_000,
  montantTtc: 11_800_000,
  dateEmission: '2026-09-01',
  dateEcheance: '2026-10-01',
  statut: 'RECUE',
  enRetard: true,
}
const page = <T,>(content: T[]) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
})

const server = setupServer(
  http.get(`${API}/finance/synthese`, () => HttpResponse.json(synthese)),
  http.get(`${API}/engagements`, () => HttpResponse.json(page([engagement]))),
  http.get(`${API}/factures`, () => HttpResponse.json(page([facture]))),
)

beforeAll(() => {
  configureHttp({ baseUrl: '/api', getToken: () => Promise.resolve(null) })
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => {
  server.resetHandlers()
})
afterAll(() => {
  server.close()
})

describe('FinancePage', () => {
  it('affiche la synthese et permet au DAF de mandater une facture', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/factures/fac-1/mandater`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ ...facture, statut: 'MANDATEE', referenceMandat: 'MDT-001' })
      }),
    )
    const user = userEvent.setup()
    renderWithProviders(<FinancePage />, { user: testUser(['DAF']), route: '/finance?onglet=factures' })

    expect(await screen.findByText(/^250.000.000.FCFA$/)).toBeInTheDocument()
    const table = await screen.findByRole('table', { name: 'Factures fournisseurs' })
    const row = within(table).getByText('FAC-2026-0042').closest('tr')!
    expect(within(row).getByText('En retard')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: 'Mandater' }))
    const dialog = await screen.findByRole('dialog', { name: /Mandater la facture FAC-2026-0042/ })
    await user.click(within(dialog).getByRole('button', { name: 'Mandater' }))
    expect(await within(dialog).findByText('Référence de mandat obligatoire')).toBeInTheDocument()
    expect(body).toBeUndefined()

    await user.type(within(dialog).getByLabelText(/Référence du mandat/), '  MDT-001 ')
    await user.click(within(dialog).getByRole('button', { name: 'Mandater' }))
    await waitFor(() => {
      expect(body).toEqual({ referenceMandat: 'MDT-001' })
    })
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('exige un motif pour refuser un engagement et envoie la decision', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/engagements/ctr-1/valider`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({})
      }),
    )
    const user = userEvent.setup()
    renderWithProviders(<FinancePage />, { user: testUser(['DAF']), route: '/finance' })

    const table = await screen.findByRole('table', { name: 'Engagements budgétaires' })
    await within(table).findByText('CTR-2026-001')
    await user.click(within(table).getByRole('button', { name: 'Refuser' }))
    const dialog = await screen.findByRole('dialog', { name: /Refuser l'engagement CTR-2026-001/ })
    await user.click(within(dialog).getByRole('button', { name: 'Refuser' }))
    expect(await within(dialog).findByText(/motif du refus est obligatoire/)).toBeInTheDocument()
    expect(body).toBeUndefined()

    await user.type(within(dialog).getByLabelText(/Motif du refus/), 'Budget chantier épuisé')
    await user.click(within(dialog).getByRole('button', { name: 'Refuser' }))
    await waitFor(() => {
      expect(body).toEqual({ approuve: false, commentaire: 'Budget chantier épuisé' })
    })
  })

  it('valide un engagement sans commentaire obligatoire', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/engagements/ctr-1/valider`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({})
      }),
    )
    const user = userEvent.setup()
    renderWithProviders(<FinancePage />, { user: testUser(['DAF']), route: '/finance' })
    const table = await screen.findByRole('table', { name: 'Engagements budgétaires' })
    await within(table).findByText('CTR-2026-001')
    await user.click(within(table).getByRole('button', { name: 'Valider' }))
    const dialog = await screen.findByRole('dialog', { name: /Valider l'engagement/ })
    await user.click(within(dialog).getByRole('button', { name: 'Valider' }))
    await waitFor(() => {
      expect(body).toEqual({ approuve: true })
    })
  })

  it("n'affiche aucune action a la Direction Generale (lecture seule, sans factures)", async () => {
    const user = userEvent.setup()
    renderWithProviders(<FinancePage />, { user: testUser(['DIRECTION_GENERALE']), route: '/finance' })
    const table = await screen.findByRole('table', { name: 'Engagements budgétaires' })
    await within(table).findByText('CTR-2026-001')
    expect(within(table).queryByRole('button', { name: 'Valider' })).not.toBeInTheDocument()
    expect(within(table).queryByRole('button', { name: 'Refuser' })).not.toBeInTheDocument()
    expect(screen.getAllByText(/Consultation seule/).length).toBeGreaterThan(0)

    await user.click(screen.getByRole('tab', { name: /Factures/ }))
    expect(await screen.findByText(/Les factures sont gérées par la DAF/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Mandater' })).not.toBeInTheDocument()
  })
})
