import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '@/test/render'
import { CloturerDialog } from './CloturerDialog'

const API = `${window.location.origin}/api`
let corps: unknown = null
const server = setupServer(
  http.patch(`${API}/maintenances/m1/cloturer`, async ({ request }) => {
    corps = await request.json()
    return HttpResponse.json({ id: 'm1', statut: 'CLOTUREE' })
  }),
)
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => {
  server.resetHandlers()
  corps = null
})
afterAll(() => {
  server.close()
})

describe('CloturerDialog', () => {
  it('exige le coût puis envoie coût, compteur et observations', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    renderWithProviders(
      <CloturerDialog
        maintenance={{ id: 'm1', numeroOt: 'OT-2026-0012', materielCode: 'PEL-001' }}
        onClose={onClose}
      />,
    )
    expect(screen.getByRole('dialog', { name: /OT-2026-0012/ })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: "Clôturer l'OT" }))
    expect(await screen.findByText('Coût obligatoire')).toBeInTheDocument()
    expect(corps).toBeNull()

    await user.type(screen.getByLabelText(/Coût total/), '350000')
    await user.type(screen.getByLabelText(/Relevé du compteur/), '1250')
    await user.type(screen.getByLabelText(/Observations/), 'Filtres remplacés')
    await user.click(screen.getByRole('button', { name: "Clôturer l'OT" }))
    await waitFor(() => {
      expect(corps).toEqual({ cout: 350000, heuresCompteur: 1250, observations: 'Filtres remplacés' })
    })
    await waitFor(() => {
      expect(onClose).toHaveBeenCalled()
    })
  })
})
