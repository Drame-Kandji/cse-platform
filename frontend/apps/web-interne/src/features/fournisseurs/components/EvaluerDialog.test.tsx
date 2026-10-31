import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, testUser } from '@/test/render'
import { evaluationSchema } from '../schemas/evaluation.schema'
import { API, fournisseurActif, HttpResponse, mswHttp, setupMsw } from '../test/fixtures'
import { EvaluerDialog } from './EvaluerDialog'

const server = setupMsw()
const id = fournisseurActif.id

describe('evaluationSchema', () => {
  it('borne chaque note entre 0 et 10', () => {
    const base = { qualite: 5, delais: 5, conformite: 5, sav: 5, prix: 5 }
    expect(evaluationSchema.safeParse(base).success).toBe(true)
    expect(evaluationSchema.safeParse({ ...base, qualite: 10.5 }).success).toBe(false)
    expect(evaluationSchema.safeParse({ ...base, prix: -1 }).success).toBe(false)
    expect(evaluationSchema.safeParse({ ...base, sav: Number.NaN }).success).toBe(false)
    expect(evaluationSchema.safeParse({ ...base, delais: 7.5 }).success).toBe(false)
  })
})

describe('EvaluerDialog', () => {
  it('borne les curseurs et refuse une note superieure a 10', async () => {
    let posted = false
    server.use(
      mswHttp.post(`${API}/fournisseurs/${id}/evaluer`, () => {
        posted = true
        return HttpResponse.json({}, { status: 201 })
      }),
    )
    const user = userEvent.setup()
    renderWithProviders(<EvaluerDialog fournisseurId={id} />, { user: testUser(['RESPONSABLE_ACHATS']) })
    await user.click(screen.getByRole('button', { name: 'Évaluer' }))

    const sliders = screen.getAllByRole('slider')
    expect(sliders).toHaveLength(5)
    for (const slider of sliders) {
      expect(slider).toHaveAttribute('min', '0')
      expect(slider).toHaveAttribute('max', '10')
    }

    const qualite = screen.getByRole('spinbutton', { name: /^Qualité/ })
    await user.clear(qualite)
    await user.type(qualite, '12')
    await user.click(screen.getByRole('button', { name: "Enregistrer l'évaluation" }))

    expect(await screen.findByText('La note doit être comprise entre 0 et 10')).toBeInTheDocument()
    expect(posted).toBe(false)
  })

  it('envoie les cinq notes valides', async () => {
    let body: unknown
    server.use(
      mswHttp.post(`${API}/fournisseurs/${id}/evaluer`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ id: 'e1' }, { status: 201 })
      }),
      mswHttp.get(`${API}/fournisseurs/${id}/evaluations`, () => HttpResponse.json([])),
    )
    const user = userEvent.setup()
    renderWithProviders(<EvaluerDialog fournisseurId={id} />, { user: testUser(['RESPONSABLE_MATERIEL']) })
    await user.click(screen.getByRole('button', { name: 'Évaluer' }))
    const prix = screen.getByRole('spinbutton', { name: /^Prix/ })
    await user.clear(prix)
    await user.type(prix, '8')
    await user.click(screen.getByRole('button', { name: "Enregistrer l'évaluation" }))
    await waitFor(() => {
      expect(body).toEqual({ qualite: 5, delais: 5, conformite: 5, sav: 5, prix: 8 })
    })
  })
})
