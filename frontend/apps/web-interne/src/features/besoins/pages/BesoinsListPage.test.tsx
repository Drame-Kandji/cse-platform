import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { renderWithProviders, testUser } from '@/test/render'
import { api, besoinFixture, chantierFixture, page } from '../testing/fixtures'
import { Component as BesoinsListPage } from './BesoinsListPage'

const requetes: URLSearchParams[] = []
const server = setupServer(
  http.get(api('/besoins'), ({ request }) => {
    requetes.push(new URL(request.url).searchParams)
    return HttpResponse.json(
      page([besoinFixture(), besoinFixture({ id: 'b2', reference: 'BES-2026-0002', statut: 'REJETE' })], 42),
    )
  }),
  http.get(api('/besoins/statistiques'), () =>
    HttpResponse.json({
      total: 42,
      parStatut: { EN_ATTENTE: 7, VALIDE: 3, AFFECTE_DIRECT: 20, EN_ACHAT: 8, REJETE: 4 },
      parPriorite: { NORMAL: 30, URGENT: 10, CRITIQUE: 2 },
      delaiMoyenValidationHeures: 18,
    }),
  ),
  http.get(api('/chantiers'), () => HttpResponse.json(page([chantierFixture()]))),
)
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => {
  server.resetHandlers()
  requetes.length = 0
})
afterAll(() => {
  server.close()
})

const derniere = () => requetes.at(-1)

describe('BesoinsListPage', () => {
  it('affiche la page serveur, les statistiques et le bouton de création pour un chef', async () => {
    renderWithProviders(<BesoinsListPage />, { user: testUser(['CHEF_CHANTIER']), route: '/besoins' })
    expect(await screen.findByText('BES-2026-0001')).toBeInTheDocument()
    expect(screen.getByText('BES-2026-0002')).toBeInTheDocument()
    expect(screen.getByText(/1–20 sur 42/)).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: /En attente\s*7/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Tous\s*42/ })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('link', { name: /Nouveau besoin/ })).toBeInTheDocument()
    expect(derniere()?.get('page')).toBe('0')
    expect(derniere()?.get('sort')).toBe('createdAt,desc')
  })

  it('met à jour la requête quand la recherche ou un filtre change', async () => {
    const user = userEvent.setup()
    renderWithProviders(<BesoinsListPage />, { user: testUser(['RESPONSABLE_ACHATS']), route: '/besoins' })
    await screen.findByText('BES-2026-0001')
    expect(screen.queryByRole('link', { name: /Nouveau besoin/ })).not.toBeInTheDocument()

    await user.type(screen.getByRole('searchbox'), 'pelle')
    await waitFor(() => {
      expect(derniere()?.get('q')).toBe('pelle')
    })

    await user.click(screen.getByRole('button', { name: /Rejetés/ }))
    await waitFor(() => {
      expect(derniere()?.getAll('statut')).toEqual(['REJETE']) // une seule valeur (filtre mono-valeur)
    })
    expect(derniere()?.get('q')).toBe('pelle')

    // Re-cliquer l'onglet actif retire le filtre.
    await user.click(screen.getByRole('button', { name: /Rejetés/ }))
    await waitFor(() => {
      expect(derniere()?.has('statut')).toBe(false)
    })
  })
})
