import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { addDays } from 'date-fns'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/shared/api'
import { renderWithProviders, testUser } from '@/test/render'
import { besoinDefaults } from '../schemas/besoin.schema'
import { api, chantierFixture, page } from '../testing/fixtures'
import { BesoinForm } from './BesoinForm'

const server = setupServer(http.get(api('/chantiers'), () => HttpResponse.json(page([chantierFixture()]))))
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => {
  server.resetHandlers()
})
afterAll(() => {
  server.close()
})

const defaults = {
  ...besoinDefaults(),
  categorie: 'MATERIEL_ROULANT' as const,
  designation: 'Pelle hydraulique 20 t',
  dateBesoin: addDays(new Date(), 10),
}

function renderForm(onSubmit = vi.fn(() => Promise.resolve())) {
  renderWithProviders(
    <BesoinForm
      defaultValues={defaults}
      submitLabel="Envoyer le besoin"
      onSubmit={onSubmit}
      onCancel={vi.fn()}
    />,
    { user: testUser(['CHEF_CHANTIER']) },
  )
  return onSubmit
}

describe('BesoinForm', () => {
  it('R08 : bloque une priorité critique sans justification puis envoie la justification', async () => {
    const user = userEvent.setup()
    const onSubmit = renderForm()
    // Chef rattache a un seul chantier : selection automatique.
    expect(await screen.findByText(/CH-DKR-01/)).toBeInTheDocument()

    await user.click(screen.getByRole('radio', { name: /Critique/ }))
    await user.click(screen.getByRole('button', { name: 'Envoyer le besoin' }))
    expect(await screen.findByText(/Justification obligatoire/)).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()

    await user.type(screen.getByLabelText(/^Justification/), 'Pelle principale en panne, terrassement arrêté')
    await user.click(screen.getByRole('button', { name: 'Envoyer le besoin' }))
    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledTimes(1)
    })
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        priorite: 'CRITIQUE',
        chantierId: 'c0000000-0000-0000-0000-000000000001',
        justification: 'Pelle principale en panne, terrassement arrêté',
        quantite: 1,
      }),
    )
  })

  it('reporte les erreurs de champs du ProblemDetail sur le formulaire', async () => {
    const user = userEvent.setup()
    renderForm(
      vi.fn(() =>
        Promise.reject(
          new ApiError(400, { errors: [{ field: 'designation', message: 'Désignation trop vague' }] }),
        ),
      ),
    )
    await screen.findByText(/CH-DKR-01/)
    await user.click(screen.getByRole('button', { name: 'Envoyer le besoin' }))
    expect(await screen.findByText('Désignation trop vague')).toBeInTheDocument()
  })
})
