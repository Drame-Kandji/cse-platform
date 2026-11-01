import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { Route, Routes } from 'react-router-dom'
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest'
import { configureHttp } from '@/shared/api'
import { renderWithProviders, testUser } from '@/test/render'
import type { Contrat } from '../api/types'
import { livraisonEnRetard } from '../components/contrat-domain'
import { Component as ContratDetailPage } from './ContratDetailPage'
import { Component as ContratsPage } from './ContratsPage'

const API = `${window.location.origin}/api`
const server = setupServer()

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' })
})
beforeEach(() => {
  configureHttp({ baseUrl: '/api', getToken: () => Promise.resolve(null) })
})
afterEach(() => {
  server.resetHandlers()
})
afterAll(() => {
  server.close()
})

function contrat(overrides: Partial<Contrat> = {}): Contrat {
  return {
    id: 'ctr-1',
    numero: 'CTR-2026-001',
    appelOffreId: 'ao-1',
    appelOffreReference: 'AO-2026-001',
    offreId: 'of-1',
    fournisseurId: 'f0000000-0000-0000-0000-000000000001',
    fournisseurNom: 'Sahel Engins SA',
    chantierNom: 'Extension autoroute Dakar-AIBD',
    objet: 'Fourniture de deux pelles hydrauliques',
    designation: 'Pelle hydraulique 20 t',
    categorie: 'MATERIEL_ROULANT',
    quantite: 2,
    montant: 120_000_000,
    statut: 'SIGNE',
    delaiLivraisonJours: 45,
    dateLivraisonPrevue: '2099-01-15',
    clauses: { garantieMois: 12, penaliteRetardPctParJour: 0.5 },
    engagement: { statut: 'EN_ATTENTE', montant: 120_000_000, dateDemande: '2026-10-01T09:00:00Z' },
    jalons: [],
    penalitesRetard: 0,
    version: 1,
    ...overrides,
  }
}

function renderDetail(c: Contrat, roles: Parameters<typeof testUser>[0] = ['RESPONSABLE_ACHATS']) {
  server.use(http.get(`${API}/contrats/${c.id}`, () => HttpResponse.json(c)))
  return renderWithProviders(
    <Routes>
      <Route path="/contrats/:id" element={<ContratDetailPage />} />
    </Routes>,
    { user: testUser(roles), route: `/contrats/${c.id}` },
  )
}

describe('ContratDetailPage', () => {
  it('apres la signature fournisseur, attend la validation DAF sans action de livraison (R06)', async () => {
    renderDetail(contrat({ statut: 'SIGNE' }))
    expect(
      await screen.findByText("En attente de la validation de l'engagement par le DAF"),
    ).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Démarrer la livraison/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Autres actions' })).not.toBeInTheDocument()
  })

  it('engagement valide : attend l expedition, demarrage hors portail dans le menu', async () => {
    const user = userEvent.setup()
    let called = false
    const c = contrat({
      statut: 'ENGAGEMENT_VALIDE',
      engagement: { ...contrat().engagement!, statut: 'APPROUVEE' },
    })
    server.use(
      http.post(`${API}/contrats/${c.id}/livraison/demarrer`, () => {
        called = true
        return HttpResponse.json({ ...c, statut: 'EN_LIVRAISON' })
      }),
    )
    renderDetail(c)
    expect(await screen.findByText("En attente de l'expédition par le fournisseur")).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Démarrer la livraison/ })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Autres actions' }))
    await user.click(await screen.findByRole('menuitem', { name: 'Démarrer la livraison (hors portail)' }))
    const dialog = await screen.findByRole('alertdialog')
    await user.click(within(dialog).getByRole('button', { name: 'Démarrer la livraison' }))
    await waitFor(() => {
      expect(called).toBe(true)
    })
    expect(await screen.findByRole('button', { name: /Confirmer la livraison/ })).toBeInTheDocument()
  })

  it('le RA signe et envoie le brouillon au fournisseur pour contresignature', async () => {
    const user = userEvent.setup()
    const c = contrat({ statut: 'BROUILLON', engagement: null })
    server.use(
      http.post(`${API}/contrats/${c.id}/signer`, () =>
        HttpResponse.json({
          ...c,
          statut: 'EN_ATTENTE_SIGNATURE',
          signatureCse: { parNom: 'Fatou Sarr', date: '2026-11-01T10:00:00Z' },
        }),
      ),
    )
    renderDetail(c)
    await user.click(await screen.findByRole('button', { name: 'Signer et envoyer au fournisseur' }))
    const dialog = await screen.findByRole('alertdialog')
    expect(within(dialog).getByText(/Le fournisseur doit contresigner/)).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Signer et envoyer au fournisseur' }))
    expect(await screen.findByText('En attente de la signature du fournisseur')).toBeInTheDocument()
    expect(screen.getByText('Fatou Sarr')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Signer et envoyer/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Modifier les clauses/ })).not.toBeInTheDocument()
  })

  it('affiche le motif de refus du fournisseur sur le brouillon', async () => {
    renderDetail(
      contrat({ statut: 'BROUILLON', engagement: null, motifRefusSignature: 'Délai de 30 jours intenable' }),
    )
    expect(await screen.findByText(/Signature refusée par le fournisseur/)).toBeInTheDocument()
    expect(screen.getByText('Délai de 30 jours intenable')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Signer et envoyer au fournisseur' })).toBeEnabled()
  })

  it('affiche les deux signatures et l expedition declaree par le fournisseur', async () => {
    renderDetail(
      contrat({
        statut: 'EN_LIVRAISON',
        signatureCse: { parNom: 'Fatou Sarr', date: '2026-10-01T09:00:00Z' },
        signatureFournisseur: { parNom: 'Moussa Kane', date: '2026-10-02T11:00:00Z' },
        dateExpedition: '2026-10-20',
        commentaireExpedition: 'Transport par porte-char',
      }),
    )
    expect(await screen.findByText('Signature CSE')).toBeInTheDocument()
    expect(screen.getByText('Fatou Sarr')).toBeInTheDocument()
    expect(screen.getByText('Signature fournisseur')).toBeInTheDocument()
    expect(screen.getByText('Moussa Kane')).toBeInTheDocument()
    expect(screen.getByText('Expédition')).toBeInTheDocument()
    expect(screen.getByText('Transport par porte-char')).toBeInTheDocument()
    expect(screen.queryByText(/En attente de/)).not.toBeInTheDocument()
  })

  it('reste en lecture seule pour le DAF', async () => {
    renderDetail(contrat({ statut: 'BROUILLON' }), ['DAF'])
    expect(await screen.findByText(/Consultation seule/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Signer et envoyer/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Modifier les clauses/ })).not.toBeInTheDocument()
  })

  it('propose signature et edition des clauses au RA sur un brouillon', async () => {
    renderDetail(contrat({ statut: 'BROUILLON', engagement: null }))
    expect(await screen.findByRole('button', { name: 'Signer et envoyer au fournisseur' })).toBeEnabled()
    expect(screen.getByRole('button', { name: /Modifier les clauses/ })).toBeInTheDocument()
  })

  it('apres un refus DAF, le brouillon affiche le motif et permet de re-signer', async () => {
    renderDetail(
      contrat({
        statut: 'BROUILLON',
        engagement: {
          statut: 'REFUSEE',
          montant: 120_000_000,
          dateDemande: '2026-10-01T09:00:00Z',
          commentaire: 'Budget chantier insuffisant',
        },
      }),
    )
    expect(await screen.findByText(/Engagement refusé par le DAF/)).toBeInTheDocument()
    expect(screen.getByText('Budget chantier insuffisant')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Signer et envoyer au fournisseur' })).toBeEnabled()
    expect(screen.queryByRole('button', { name: /Démarrer la livraison/ })).not.toBeInTheDocument()
  })

  it('signale les penalites de retard', async () => {
    renderDetail(
      contrat({ statut: 'EN_LIVRAISON', dateLivraisonPrevue: '2026-01-01', penalitesRetard: 3_000_000 }),
    )
    expect(await screen.findByText('Livraison en retard')).toBeInTheDocument()
    expect(screen.getByText(/3\s000\s000\sFCFA/)).toBeInTheDocument()
  })
})

describe('ContratsPage', () => {
  it('affiche la liste des contrats', async () => {
    server.use(
      http.get(`${API}/contrats`, () =>
        HttpResponse.json({
          content: [contrat()],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        }),
      ),
      http.get(`${API}/fournisseurs`, () =>
        HttpResponse.json({
          content: [],
          page: 0,
          size: 50,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
        }),
      ),
    )
    renderWithProviders(<ContratsPage />, { user: testUser(['RESPONSABLE_ACHATS']), route: '/contrats' })
    expect(await screen.findByText('CTR-2026-001')).toBeInTheDocument()
    expect(screen.getByText('Sahel Engins SA')).toBeInTheDocument()
    const montant = screen.getByRole('columnheader', { name: 'Montant' })
    expect(within(montant).queryByRole('button')).not.toBeInTheDocument()
    expect(
      within(screen.getByRole('columnheader', { name: /Numéro/ })).getByRole('button'),
    ).toBeInTheDocument()
  })
})

describe('livraisonEnRetard', () => {
  it('detecte une date prevue depassee sans livraison', () => {
    const now = new Date('2026-10-30T10:00:00')
    expect(livraisonEnRetard(contrat({ dateLivraisonPrevue: '2026-10-29' }), now)).toBe(true)
    expect(livraisonEnRetard(contrat({ dateLivraisonPrevue: '2026-10-30' }), now)).toBe(false)
    expect(
      livraisonEnRetard(
        contrat({ statut: 'LIVRE', dateLivraisonPrevue: '2026-10-01', dateLivraisonReelle: '2026-09-30' }),
        now,
      ),
    ).toBe(false)
  })
})
