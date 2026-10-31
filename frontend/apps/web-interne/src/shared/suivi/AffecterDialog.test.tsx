import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '@/test/render'
import { AffecterDialog } from './AffecterDialog'

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

const emptyPage = { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0, first: true, last: true }
const CHANTIER = {
  id: 'c0000000-0000-0000-0000-000000000002',
  code: 'CH-THS-01',
  nom: 'Contournement de Thiès RN2',
  localisation: 'Thiès',
  region: 'Thiès',
  latitude: 14.791,
  longitude: -16.9256,
  statut: 'EN_COURS',
  budget: 1,
  montantMarche: 1,
  dateDebut: '2026-01-01',
  nombreMateriels: 4,
  version: 0,
}

function mockReferentiels() {
  server.use(
    http.get(`${API}/chantiers`, () =>
      HttpResponse.json({ ...emptyPage, content: [CHANTIER], totalElements: 1 }),
    ),
    http.get(`${API}/besoins`, () => HttpResponse.json(emptyPage)),
    http.get(`${API}/materiels`, () => HttpResponse.json(emptyPage)),
  )
}

const materiel = { id: 'm1', code: 'ENG-PEL-01', designation: 'Pelle hydraulique' }

function renderDialog(etat: 'DISPONIBLE' | 'AFFECTE', onOpenChange = vi.fn()) {
  renderWithProviders(
    <AffecterDialog
      open
      onOpenChange={onOpenChange}
      materiel={{ ...materiel, etat }}
      defaultChantierId={CHANTIER.id}
      defaultDateDebut={new Date('2026-11-02T00:00:00')}
      defaultDateFinPrevue={new Date('2026-11-20T00:00:00')}
    />,
  )
  return onOpenChange
}

describe('AffecterDialog', () => {
  it('affiche le conflit de periode (409 CONFLIT_AFFECTATION) dans le dialogue sans le fermer', async () => {
    mockReferentiels()
    let body: unknown
    server.use(
      http.post(`${API}/materiels/m1/affecter`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json(
          {
            status: 409,
            code: 'CONFLIT_AFFECTATION',
            title: 'Conflit',
            detail: 'Le matériel est déjà affecté du 10/11/2026 au 30/11/2026.',
          },
          { status: 409, headers: { 'content-type': 'application/problem+json' } },
        )
      }),
    )
    const onOpenChange = renderDialog('DISPONIBLE')
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Affecter' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Période déjà occupée')
    expect(alert).toHaveTextContent('déjà affecté du 10/11/2026 au 30/11/2026')
    expect(body).toEqual({
      chantierId: CHANTIER.id,
      dateDebut: '2026-11-02',
      dateFinPrevue: '2026-11-20',
    })
    expect(onOpenChange).not.toHaveBeenCalledWith(false)
  })

  it('ferme le dialogue apres une affectation reussie', async () => {
    mockReferentiels()
    server.use(
      http.post(`${API}/materiels/m1/affecter`, () =>
        HttpResponse.json({ id: 'a1', statut: 'EN_ATTENTE_RECEPTION' }, { status: 201 }),
      ),
    )
    const onOpenChange = renderDialog('DISPONIBLE')
    await userEvent.setup().click(screen.getByRole('button', { name: 'Affecter' }))
    await waitFor(() => {
      expect(onOpenChange).toHaveBeenCalledWith(false)
    })
  })

  it("desactive l'affectation d'un materiel non DISPONIBLE (R04)", async () => {
    mockReferentiels()
    renderDialog('AFFECTE')
    expect(screen.getByRole('button', { name: 'Affecter' })).toBeDisabled()
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Seul un matériel disponible peut être affecté',
    )
  })
})
