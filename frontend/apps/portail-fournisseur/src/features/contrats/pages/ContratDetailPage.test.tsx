import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { contrat, facture } from '@/test/fixtures'
import { renderPortail } from '@/test/render'
import { api, http, HttpResponse, page, problem, server } from '@/test/server'
import { Component as ContratDetailPage } from './ContratDetailPage'

function rendre() {
  return renderPortail(<ContratDetailPage />, { route: '/contrats/ct-1', path: '/contrats/:id' })
}

describe('Detail contrat : signature', () => {
  it('confirme en rappelant montant et delai, signe puis affiche le nouveau statut', async () => {
    let signe = false
    server.use(
      http.get(api('/contrats/ct-1'), () => HttpResponse.json(contrat())),
      http.get(api('/factures'), () => HttpResponse.json(page([]))),
      http.post(api('/contrats/ct-1/signer'), () => {
        signe = true
        return HttpResponse.json(
          contrat({ statut: 'SIGNE', signatureFournisseur: { parNom: 'Moussa Kane', date: '2026-11-01' } }),
        )
      }),
    )
    const user = userEvent.setup()
    rendre()
    expect(
      await screen.findByRole('heading', { name: 'Acquisition de deux pelles hydrauliques' }),
    ).toBeInTheDocument()
    expect(screen.getByText('À signer')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Signer le contrat' }))
    const dialog = await screen.findByRole('alertdialog')
    expect(within(dialog).getByText(/285\s000\s000\sFCFA/)).toBeInTheDocument()
    expect(within(dialog).getByText(/sous 45 jours/)).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Signer' }))

    expect(await screen.findByText('Signé')).toBeInTheDocument()
    expect(signe).toBe(true)
    expect(screen.queryByRole('button', { name: 'Signer le contrat' })).not.toBeInTheDocument()
  })

  it('refus : motif obligatoire puis envoi', async () => {
    let motif: unknown
    server.use(
      http.get(api('/contrats/ct-1'), () => HttpResponse.json(contrat())),
      http.get(api('/factures'), () => HttpResponse.json(page([]))),
      http.post(api('/contrats/ct-1/refuser'), async ({ request }) => {
        motif = await request.json()
        return HttpResponse.json(contrat({ statut: 'BROUILLON', motifRefusSignature: 'Délai trop court' }))
      }),
    )
    const user = userEvent.setup()
    rendre()
    await user.click(await screen.findByRole('button', { name: 'Autres actions' }))
    await user.click(await screen.findByRole('menuitem', { name: 'Refuser la signature' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Refuser' }))
    expect(await within(dialog).findByText(/Motif obligatoire/)).toBeInTheDocument()
    await user.type(within(dialog).getByLabelText(/Motif du refus/), 'Délai trop court')
    await user.click(within(dialog).getByRole('button', { name: 'Refuser' }))
    expect(await screen.findByText('Signature refusée : Délai trop court')).toBeInTheDocument()
    expect(motif).toEqual({ motif: 'Délai trop court' })
  })

  it('transition invalide : message francais, statut inchange', async () => {
    server.use(
      http.get(api('/contrats/ct-1'), () => HttpResponse.json(contrat())),
      http.get(api('/factures'), () => HttpResponse.json(page([]))),
      http.post(api('/contrats/ct-1/signer'), () => problem(409, 'TRANSITION_INTERDITE')),
    )
    const user = userEvent.setup()
    rendre()
    await user.click(await screen.findByRole('button', { name: 'Signer le contrat' }))
    await user.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Signer' }))
    expect(await screen.findByRole('button', { name: 'Signer le contrat' })).toBeInTheDocument()
    expect(screen.getByText('À signer')).toBeInTheDocument()
  })

  it('contrat livre sans facture : bouton de depot ; livre facture : aucun bouton', async () => {
    server.use(
      http.get(api('/contrats/ct-1'), () => HttpResponse.json(contrat({ statut: 'LIVRE' }))),
      http.get(api('/factures'), () => HttpResponse.json(page([facture({ statut: 'REJETEE' })]))),
    )
    rendre()
    expect(await screen.findByRole('button', { name: 'Déposer la facture' })).toBeInTheDocument()
  })

  it('contrat introuvable (404) : etat vide', async () => {
    server.use(
      http.get(api('/contrats/ct-1'), () => problem(404, 'INTROUVABLE')),
      http.get(api('/factures'), () => HttpResponse.json(page([]))),
    )
    rendre()
    expect(await screen.findByText('Contrat introuvable')).toBeInTheDocument()
  })
})
