import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '@/test/render'
import { buildUtilisateurPayload } from '../lib/payload'
import type { UtilisateurCreation } from '../types'
import { UserFormDialog } from './UserFormDialog'

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

const chantiers = {
  content: [
    { id: 'c1', code: 'CH-DKR-01', nom: 'Extension autoroute Dakar–AIBD', statut: 'EN_COURS' },
    { id: 'c2', code: 'CH-THS-01', nom: 'Contournement de Thiès RN2', statut: 'EN_COURS' },
  ],
  page: 0,
  size: 50,
  totalElements: 2,
  totalPages: 1,
  first: true,
  last: true,
}

describe('UserFormDialog', () => {
  it('adapte les champs au role choisi', async () => {
    server.use(http.get(`${API}/chantiers`, () => HttpResponse.json(chantiers)))
    const user = userEvent.setup()
    renderWithProviders(<UserFormDialog open onOpenChange={vi.fn()} />)

    // Chef de chantier (defaut) : selection des chantiers.
    expect(await screen.findByText('Extension autoroute Dakar–AIBD')).toBeInTheDocument()
    expect(screen.queryByLabelText(/Seuil de délégation/)).not.toBeInTheDocument()

    await user.click(screen.getByRole('radio', { name: /Responsable Achats/ }))
    expect(screen.getByLabelText(/Seuil de délégation/)).toBeInTheDocument()
    expect(screen.queryByText('Chantiers rattachés')).not.toBeInTheDocument()

    await user.click(screen.getByRole('radio', { name: /Directeur administratif et financier/ }))
    expect(screen.getByLabelText(/Budget géré/)).toBeInTheDocument()
    expect(screen.queryByLabelText(/Seuil de délégation/)).not.toBeInTheDocument()

    await user.click(screen.getByRole('radio', { name: /Responsable Matériel/ }))
    expect(screen.getByLabelText(/Périmètre du parc/)).toBeInTheDocument()

    await user.click(screen.getByRole('radio', { name: /Direction Générale/ }))
    expect(screen.queryByLabelText(/Périmètre du parc/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/Budget géré/)).not.toBeInTheDocument()
  })

  it("exige un chantier pour un chef et n'envoie que les attributs de son role", async () => {
    let body: UtilisateurCreation | undefined
    server.use(
      http.get(`${API}/chantiers`, () => HttpResponse.json(chantiers)),
      http.post(`${API}/admin/utilisateurs`, async ({ request }) => {
        body = (await request.json()) as UtilisateurCreation
        return HttpResponse.json({ id: 'u-9' }, { status: 201 })
      }),
    )
    const onOpenChange = vi.fn()
    const user = userEvent.setup()
    renderWithProviders(<UserFormDialog open onOpenChange={onOpenChange} />)
    await user.type(screen.getByLabelText(/Prénom/), 'Mamadou')
    await user.type(screen.getByLabelText(/^Nom/), 'Diop')
    await user.type(screen.getByLabelText(/Adresse e-mail/), 'chef.chantier@cse.sn')
    await user.click(screen.getByRole('button', { name: "Créer l'utilisateur" }))
    expect(await screen.findByText('Rattachez au moins un chantier')).toBeInTheDocument()

    await user.click(screen.getByRole('checkbox', { name: /Contournement de Thiès/ }))
    await user.click(screen.getByRole('button', { name: "Créer l'utilisateur" }))
    await waitFor(() => {
      expect(onOpenChange).toHaveBeenCalledWith(false)
    })
    expect(body).toMatchObject({ prenom: 'Mamadou', role: 'CHEF_CHANTIER', chantierIds: ['c2'] })
    expect(body?.motDePasseTemporaire.length).toBeGreaterThanOrEqual(12)
    expect(body).not.toHaveProperty('seuilDelegation')
  })
})

describe('buildUtilisateurPayload', () => {
  it('ignore les attributs des autres roles', () => {
    const payload = buildUtilisateurPayload({
      prenom: 'Fatou',
      nom: 'Sarr',
      email: 'resp.achats@cse.sn',
      telephone: undefined,
      role: 'RESPONSABLE_ACHATS',
      chantierIds: ['c1'],
      perimetreParc: 'Dakar',
      seuilDelegation: 150_000_000,
      budgetGere: 10,
      motDePasseTemporaire: '',
    })
    expect(payload).toEqual({
      prenom: 'Fatou',
      nom: 'Sarr',
      email: 'resp.achats@cse.sn',
      role: 'RESPONSABLE_ACHATS',
      seuilDelegation: 150_000_000,
    })
  })
})
