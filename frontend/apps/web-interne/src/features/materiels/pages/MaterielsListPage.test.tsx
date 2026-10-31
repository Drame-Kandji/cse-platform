import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import type { Materiel } from '@/shared/domain/types'
import { renderWithProviders, testUser } from '@/test/render'
import { Component as MaterielsListPage } from './MaterielsListPage'

const API = `${window.location.origin}/api`
const server = setupServer()
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => {
  server.resetHandlers()
})
afterAll(() => {
  server.close()
})

const PELLE: Materiel = {
  id: 'm1',
  code: 'ENG-PEL-01',
  designation: 'Pelle hydraulique 20 t',
  categorie: 'MATERIEL_ROULANT',
  etat: 'DISPONIBLE',
  heuresFonctionnement: 4200,
  heuresDepuisMaintenance: 480,
  seuilMaintenanceHeures: 500,
  maintenanceDue: false,
  tarifJournalier: 350000,
  reformeDemandee: false,
  version: 1,
}

const page = (content: Materiel[], totalElements = content.length) => ({
  content,
  page: 0,
  size: 20,
  totalElements,
  totalPages: 1,
  first: true,
  last: true,
})

/** Capture les requetes de liste (size != 1) et repond aux compteurs par etat (size = 1). */
function mockParc() {
  const listRequests: URLSearchParams[] = []
  server.use(
    http.get(`${API}/materiels`, ({ request }) => {
      const params = new URL(request.url).searchParams
      if (params.get('size') === '1')
        return HttpResponse.json(page([], params.get('etat') === 'AFFECTE' ? 7 : 2))
      listRequests.push(params)
      return HttpResponse.json(page([PELLE]))
    }),
    http.get(`${API}/chantiers`, () => HttpResponse.json(page([]))),
  )
  return listRequests
}

describe('MaterielsListPage', () => {
  it("ignore une valeur d'etat inconnue dans l'URL", async () => {
    const requests = mockParc()
    renderWithProviders(<MaterielsListPage />, { route: '/materiels?etat=PERDU' })
    await screen.findByText('Pelle hydraulique 20 t')
    expect(requests.at(-1)?.get('etat')).toBeNull()
  })

  it("traduit les filtres de l'URL en parametres d'API (une valeur par filtre)", async () => {
    const requests = mockParc()
    renderWithProviders(<MaterielsListPage />, {
      route: '/materiels?etat=EN_MAINTENANCE&categorie=OUTILLAGE&chantier=c1&due=1&q=pelle&page=2',
    })
    expect(await screen.findByText('Pelle hydraulique 20 t')).toBeInTheDocument()
    const params = requests.at(-1)
    expect(params?.getAll('etat')).toEqual(['EN_MAINTENANCE'])
    expect(params?.getAll('categorie')).toEqual(['OUTILLAGE'])
    expect(params?.get('categorie')).toBe('OUTILLAGE')
    expect(params?.get('chantierId')).toBe('c1')
    expect(params?.get('maintenanceDue')).toBe('true')
    expect(params?.get('q')).toBe('pelle')
    expect(params?.get('page')).toBe('1')
    expect(params?.get('sort')).toBe('code,asc')
  })

  it('applique un filtre, revient en page 1 et affiche la synthese par etat', async () => {
    const requests = mockParc()
    const user = userEvent.setup()
    renderWithProviders(<MaterielsListPage />, { route: '/materiels?page=3' })
    await screen.findByText('Pelle hydraulique 20 t')
    expect(requests.at(-1)?.get('maintenanceDue')).toBeNull()

    await user.click(screen.getByRole('button', { name: 'Maintenance due' }))
    await waitFor(() => {
      expect(requests.at(-1)?.get('maintenanceDue')).toBe('true')
    })
    expect(requests.at(-1)?.get('page')).toBe('0')

    const affectes = screen.getByRole('button', { name: /Affecté\s*7/ })
    await user.click(affectes)
    await waitFor(() => {
      expect(requests.at(-1)?.getAll('etat')).toEqual(['AFFECTE'])
    })
    expect(affectes).toHaveAttribute('aria-pressed', 'true')

    // Un second clic revient a "Tous les etats".
    await user.click(affectes)
    await waitFor(() => {
      expect(requests.at(-1)?.get('etat')).toBeNull()
    })
  })

  it('reserve la creation de materiel au Responsable Materiel', async () => {
    mockParc()
    renderWithProviders(<MaterielsListPage />, {
      user: testUser(['DIRECTION_GENERALE']),
      route: '/materiels',
    })
    await screen.findByText('Pelle hydraulique 20 t')
    expect(screen.queryByRole('button', { name: 'Nouveau matériel' })).not.toBeInTheDocument()
  })
})
