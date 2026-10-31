import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { renderWithProviders, testUser } from '@/test/render'
import { Component as AuditPage } from './AuditPage'

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

const page = <T,>(content: T[]) => ({
  content,
  page: 0,
  size: 50,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
})

describe('AuditPage', () => {
  it('traduit les filtres en parametres de requete et ouvre le detail', async () => {
    const requests: URLSearchParams[] = []
    server.use(
      http.get(`${API}/audit/statistiques`, () =>
        HttpResponse.json({
          total: 120,
          aujourdhui: 4,
          septDerniersJours: 30,
          parService: [{ service: 'ms-besoins', total: 80 }],
        }),
      ),
      http.get(`${API}/audit`, ({ request }) => {
        requests.push(new URL(request.url).searchParams)
        return HttpResponse.json(
          page([
            {
              id: 'a1',
              eventId: 'evt-1',
              occurredAt: '2026-10-30T09:15:00Z',
              service: 'ms-besoins',
              action: 'BESOIN_VALIDE',
              entite: 'Besoin',
              entiteId: 'b-1',
              utilisateurNom: 'Ibrahima Fall',
              roles: ['RESPONSABLE_MATERIEL'],
              resume: 'Besoin BES-2026-0001 validé',
              correlationId: 'corr-42',
            },
          ]),
        )
      }),
      http.get(`${API}/admin/utilisateurs`, () => HttpResponse.json(page([]))),
    )
    const user = userEvent.setup()
    renderWithProviders(<AuditPage />, {
      user: testUser(['ADMIN_SYSTEME']),
      route: '/admin/audit?service=ms-besoins',
    })

    await screen.findAllByText('BESOIN_VALIDE')
    expect(requests[0]?.get('service')).toBe('ms-besoins')
    expect(requests[0]?.get('sort')).toBe('occurredAt,desc')
    expect(requests[0]?.get('page')).toBe('0')

    await user.type(screen.getByRole('searchbox', { name: 'Rechercher dans le journal…' }), 'BES-2026')
    await user.type(screen.getByRole('textbox', { name: 'Entité' }), 'Besoin')
    await waitFor(() => {
      const last = requests[requests.length - 1]
      expect(last?.get('q')).toBe('BES-2026')
      expect(last?.get('entite')).toBe('Besoin')
      expect(last?.get('service')).toBe('ms-besoins')
    })

    await user.click(screen.getAllByText('Besoin BES-2026-0001 validé')[0]!)
    expect(await screen.findByText('corr-42')).toBeInTheDocument()
  })
})
