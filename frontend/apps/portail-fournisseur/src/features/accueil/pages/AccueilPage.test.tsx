import { screen } from '@testing-library/react'
import { appelOffre, contrat, facture, offre, profil } from '@/test/fixtures'
import { renderPortail } from '@/test/render'
import { api, http, HttpResponse, page, problem, server } from '@/test/server'
import { Component as AccueilPage } from './AccueilPage'

describe('Accueil', () => {
  it('salue l entreprise, liste les actions et les indicateurs', async () => {
    server.use(
      http.get(api('/profil'), () => HttpResponse.json(profil())),
      http.get(api('/appels-offres'), () => HttpResponse.json(page([appelOffre({ invite: true })]))),
      http.get(api('/offres/mes-offres'), () =>
        HttpResponse.json(page([offre(), offre({ id: 'o2', statutAppelOffre: 'ATTRIBUE' })])),
      ),
      http.get(api('/contrats'), () => HttpResponse.json(page([contrat()]))),
      http.get(api('/factures'), () =>
        HttpResponse.json(page([facture({ statut: 'PAYEE', montantTtc: 1_000_000 })])),
      ),
    )
    renderPortail(<AccueilPage />)
    expect(await screen.findByRole('heading', { name: 'Bonjour, Sahel Engins SA' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: /Signer le contrat/ })).toHaveAttribute(
      'href',
      '/contrats/ct-1',
    )
    expect(screen.getByRole('link', { name: /Déposer une offre/ })).toHaveAttribute(
      'href',
      '/appels-offres/ao-1',
    )
    expect(screen.getByText('Offres en cours').nextSibling).toHaveTextContent('1')
  })

  it('endpoints pas encore deployes (404) : etat vide, pas d erreur', async () => {
    server.use(
      http.get(api('/profil'), () => problem(404, 'X')),
      http.get(api('/appels-offres'), () => HttpResponse.json(page([]))),
      http.get(api('/offres/mes-offres'), () => HttpResponse.json(page([]))),
      http.get(api('/contrats'), () => problem(404, 'X')),
      http.get(api('/factures'), () => problem(404, 'X')),
    )
    renderPortail(<AccueilPage />)
    expect(await screen.findByText('Rien à faire pour le moment')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Bonjour, Moussa Kane' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
