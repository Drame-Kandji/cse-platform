import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { api, http, HttpResponse, problem, server } from '@/test/server'
import { renderPortail } from '@/test/render'
import { Component as InscriptionPage } from './InscriptionPage'

async function remplir(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/Raison sociale/), 'Baobab Logistique SARL')
  await user.type(screen.getByLabelText(/^NINEA/), '008789012 2E7')
  await user.type(screen.getByLabelText(/^E-mail/), 'contact@baobab.sn')
  await user.type(screen.getByLabelText(/Nom du contact/), 'Seynabou Gueye')
  await user.click(screen.getByRole('button', { name: 'Consommable' }))
}

describe('Inscription publique', () => {
  it('envoie la demande et affiche le message d attente de validation', async () => {
    let corps: unknown
    server.use(
      http.post(api('/inscription'), async ({ request }) => {
        corps = await request.json()
        return HttpResponse.json(
          { id: 'f7', raisonSociale: 'Baobab Logistique SARL', statut: 'EN_ATTENTE_VALIDATION' },
          { status: 201 },
        )
      }),
    )
    const user = userEvent.setup()
    renderPortail(<InscriptionPage />, { user: null, route: '/inscription' })
    expect(screen.getByLabelText(/^Pays/)).toHaveValue('Sénégal')
    await remplir(user)
    await user.click(screen.getByRole('button', { name: 'Envoyer la demande' }))

    expect(await screen.findByText(/en attente de validation par le service achats/)).toBeInTheDocument()
    expect(screen.getByText(/Vous recevrez un e-mail pour définir votre mot de passe/)).toBeInTheDocument()
    expect(corps).toEqual({
      raisonSociale: 'Baobab Logistique SARL',
      ninea: '008789012 2E7',
      pays: 'Sénégal',
      email: 'contact@baobab.sn',
      contactNom: 'Seynabou Gueye',
      specialites: ['CONSOMMABLE'],
    })
    expect(screen.getByRole('link', { name: 'Retour à la connexion' })).toHaveAttribute('href', '/')
  })

  it('affiche le conflit FOURNISSEUR_EXISTANT (409) sans quitter le formulaire', async () => {
    server.use(http.post(api('/inscription'), () => problem(409, 'FOURNISSEUR_EXISTANT', 'NINEA connu')))
    const user = userEvent.setup()
    renderPortail(<InscriptionPage />, { user: null, route: '/inscription' })
    await remplir(user)
    await user.click(screen.getByRole('button', { name: 'Envoyer la demande' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      /NINEA ou cette adresse e-mail est déjà inscrite/,
    )
    expect(screen.getByRole('button', { name: 'Envoyer la demande' })).toBeInTheDocument()
  })

  it('valide le formulaire avant envoi', async () => {
    const user = userEvent.setup()
    renderPortail(<InscriptionPage />, { user: null, route: '/inscription' })
    await user.click(screen.getByRole('button', { name: 'Envoyer la demande' }))
    await waitFor(() => {
      expect(screen.getByText('Choisissez au moins une spécialité')).toBeInTheDocument()
    })
    expect(screen.getByText('Raison sociale obligatoire')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Se connecter' })).toHaveAttribute('href', '/')
  })
})
