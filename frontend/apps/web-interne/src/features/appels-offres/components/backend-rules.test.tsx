import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders, testUser } from '@/test/render'
import { invitesManquants } from '../schemas/appel-offre.schema'
import { API, appelOffre, attribution, setupMsw } from '../test/fixtures'
import { AppelsOffresTable } from './AppelsOffresTable'
import { AttributionTab } from './AttributionTab'
import { SaisirOffreDialog } from './SaisirOffreDialog'

const server = setupMsw()
const page = <T,>(content: T[]) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
})

describe('regles verifiees sur le backend ms-achats', () => {
  it('invites obligatoires pour une demande de prix seulement', () => {
    expect(invitesManquants('DEMANDE_PRIX', [])).toBeDefined()
    expect(invitesManquants('DEMANDE_PRIX', ['f1'])).toBeUndefined()
    expect(invitesManquants('APPEL_OFFRES', [])).toBeUndefined()
  })

  it('envoie un seul statut et un tri autorise', async () => {
    const user = userEvent.setup()
    const requetes: URLSearchParams[] = []
    server.use(
      http.get(`${API}/appels-offres`, ({ request }) => {
        requetes.push(new URL(request.url).searchParams)
        return HttpResponse.json(page([appelOffre()]))
      }),
    )
    renderWithProviders(<AppelsOffresTable />, { user: testUser(['RESPONSABLE_ACHATS']) })
    expect(await screen.findByText('AO-2026-001')).toBeInTheDocument()
    expect(requetes[0]?.get('sort')).toBe('dateLimite,desc')
    await user.click(screen.getByRole('button', { name: /Statut/ }))
    await user.click(await screen.findByRole('option', { name: 'Publié' }))
    await user.click(await screen.findByRole('option', { name: 'Clôturé' }))
    await waitFor(() => {
      expect(requetes.at(-1)?.getAll('statut')).toEqual(['CLOTURE'])
    })
  })

  it('rattache OFFRE_DEJA_DEPOSEE au champ fournisseur', async () => {
    const user = userEvent.setup()
    server.use(
      http.get(`${API}/fournisseurs`, () => HttpResponse.json(page([]))),
      http.post(`${API}/appels-offres/ao-1/offres`, () =>
        HttpResponse.json(
          { status: 409, code: 'OFFRE_DEJA_DEPOSEE', title: 'Conflit', detail: 'Déjà déposée' },
          { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    renderWithProviders(<SaisirOffreDialog ao={appelOffre()} open onOpenChange={vi.fn()} />, {
      user: testUser(['RESPONSABLE_ACHATS']),
    })
    await user.click(screen.getByRole('combobox', { name: /Fournisseur/ }))
    await user.click(await screen.findByRole('option', { name: 'Sahel Engins SA (invité)' }))
    await user.type(screen.getByLabelText(/Montant/), '150000000')
    await user.type(screen.getByLabelText(/Délai/), '45')
    await user.click(screen.getByRole('button', { name: /Enregistrer l.offre/ }))
    expect(
      await screen.findByText('Ce fournisseur a déjà déposé une offre sur cet appel d’offres.'),
    ).toBeInTheDocument()
  })

  it('relie un AO attribue a son contrat genere', async () => {
    let q: string | null = null
    server.use(
      http.get(`${API}/contrats`, ({ request }) => {
        q = new URL(request.url).searchParams.get('q')
        return HttpResponse.json(
          page([
            { id: 'ctr-x', numero: 'CTR-2026-009', appelOffreId: 'autre-ao' },
            { id: 'ctr-1', numero: 'CTR-2026-001', appelOffreId: 'ao-1' },
          ]),
        )
      }),
    )
    const attr = attribution({ coValidationMateriel: { statut: 'APPROUVEE' } })
    renderWithProviders(<AttributionTab ao={appelOffre({ statut: 'ATTRIBUE', attribution: attr })} />, {
      user: testUser(['RESPONSABLE_ACHATS']),
    })
    const lien = await screen.findByRole('link', { name: /Voir le contrat CTR-2026-001/ })
    expect(lien).toHaveAttribute('href', '/contrats/ctr-1')
    expect(q).toBe('Fourniture de deux pelles hydrauliques')
  })
})
