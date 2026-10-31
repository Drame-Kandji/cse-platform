import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { configureHttp } from '@/shared/api'
import { renderWithProviders, testUser } from '@/test/render'
import { FacturesPanel } from './FacturesPanel'

const API = `${window.location.origin}/api`
const page = <T,>(content: T[]) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
})
const contrat = {
  id: 'ctr-1',
  numero: 'CTR-2026-001',
  fournisseurNom: 'Sahel Engins SA',
  objet: 'Pelle hydraulique',
  montant: 10_000_000,
  statut: 'LIVRE',
}

let urls: URL[] = []
const server = setupServer(
  http.get(`${API}/factures`, ({ request }) => {
    urls.push(new URL(request.url))
    return HttpResponse.json(page([]))
  }),
  http.get(`${API}/contrats`, () => HttpResponse.json(page([contrat]))),
)

beforeAll(() => {
  configureHttp({ baseUrl: '/api', getToken: () => Promise.resolve(null) })
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => {
  server.resetHandlers()
  urls = []
})
afterAll(() => {
  server.close()
})

describe('FacturesPanel', () => {
  it('envoie un seul statut et le tri par defaut autorise', async () => {
    const user = userEvent.setup()
    renderWithProviders(<FacturesPanel />, { user: testUser(['DAF']), route: '/finance?onglet=factures' })
    await waitFor(() => {
      expect(urls).toHaveLength(1)
    })
    expect(urls[0]!.searchParams.get('sort')).toBe('dateEmission,desc')

    await user.click(screen.getByRole('button', { name: /Statut/ }))
    await user.click(await screen.findByRole('option', { name: 'Reçue' }))
    await user.click(screen.getByRole('option', { name: 'Payée' }))
    await waitFor(() => {
      expect(urls.at(-1)!.searchParams.getAll('statut')).toEqual(['PAYEE'])
    })
  })

  it('affiche le depassement du montant du contrat sur le champ Montant HT', async () => {
    server.use(
      http.post(`${API}/factures`, () =>
        HttpResponse.json(
          {
            status: 422,
            code: 'DEPASSEMENT_MONTANT_CONTRAT',
            title: 'Règle métier',
            detail: 'Le cumul des factures dépasse le montant du contrat.',
          },
          { status: 422, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    const user = userEvent.setup()
    renderWithProviders(<FacturesPanel />, { user: testUser(['DAF']), route: '/finance?onglet=factures' })
    await user.click(await screen.findByRole('button', { name: 'Enregistrer une facture' }))
    const dialog = await screen.findByRole('dialog', { name: 'Enregistrer une facture' })

    await user.click(within(dialog).getByRole('combobox'))
    await user.click(await screen.findByRole('option', { name: /CTR-2026-001/ }))
    await user.type(within(dialog).getByLabelText(/Numéro de facture/), 'FAC-9')
    await user.type(within(dialog).getByLabelText(/Montant HT/), '20000000')
    await user.type(within(dialog).getByLabelText(/Montant de TVA/), '0')
    for (const label of [/Date d'émission/, /Date d'échéance/]) {
      await user.click(within(dialog).getByLabelText(label))
      const days = await screen.findAllByRole('gridcell')
      await user.click(days[0]!)
    }
    await user.click(within(dialog).getByRole('button', { name: 'Enregistrer' }))
    const montant = within(dialog).getByLabelText(/Montant HT/)
    await waitFor(() => {
      expect(montant).toHaveAttribute('aria-invalid', 'true')
    })
    expect(within(dialog).getByRole('alert')).toHaveTextContent(/dépasse le montant du contrat/)
  })
})
