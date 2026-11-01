import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { appelOffre, offre } from '@/test/fixtures'
import { renderPortail } from '@/test/render'
import { api, http, HttpResponse, problem, server } from '@/test/server'
import { DeposerOffreDialog } from './DeposerOffreDialog'

async function saisir(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/Montant proposé/), '96 000 000')
  await user.type(screen.getByLabelText(/Délai de livraison/), '30')
  await user.type(screen.getByLabelText(/Garantie/), '12')
  await user.click(screen.getByRole('button', { name: "Déposer l'offre" }))
}

describe('DeposerOffreDialog', () => {
  it('envoie montant, delai et garantie puis ferme le dialogue', async () => {
    let corps: unknown
    server.use(
      http.post(api('/appels-offres/ao-1/offres'), async ({ request }) => {
        corps = await request.json()
        return HttpResponse.json(offre(), { status: 201 })
      }),
    )
    const onOpenChange = vi.fn()
    const user = userEvent.setup()
    renderPortail(<DeposerOffreDialog appelOffre={appelOffre()} open onOpenChange={onOpenChange} />)
    await saisir(user)
    await waitFor(() => {
      expect(onOpenChange).toHaveBeenCalledWith(false)
    })
    expect(corps).toEqual({ montantPropose: 96_000_000, delaiLivraisonJours: 30, garantieMois: 12 })
  })

  it.each([
    [409, 'OFFRE_DEJA_DEPOSEE', /déjà déposé une offre/],
    [409, 'AUTRE', /déjà déposé une offre/],
    [422, 'DATE_LIMITE_DEPASSEE', /date limite de dépôt est dépassée/],
    [422, 'FOURNISSEUR_NON_ELIGIBLE', /pas éligible/],
  ])('erreur %i %s : message en ligne, dialogue ouvert', async (status, code, message) => {
    server.use(http.post(api('/appels-offres/ao-1/offres'), () => problem(status, code, 'Conflict')))
    const onOpenChange = vi.fn()
    const user = userEvent.setup()
    renderPortail(<DeposerOffreDialog appelOffre={appelOffre()} open onOpenChange={onOpenChange} />)
    await saisir(user)
    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    expect(onOpenChange).not.toHaveBeenCalledWith(false)
  })
})
