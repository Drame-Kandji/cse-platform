import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, testUser } from '@/test/render'
import { API, appelOffre, offre, setupMsw } from '../test/fixtures'
import { OffresTab } from './OffresTab'

const server = setupMsw()

/** Offres renvoyees par erreur avec les noms (le front doit malgre tout anonymiser). */
const offresNominatives = [
  offre({ id: 'of-1', codeAnonyme: 'OFF-A1', fournisseurId: 'f1', fournisseurNom: 'Sahel Engins SA' }),
  offre({
    id: 'of-2',
    codeAnonyme: 'OFF-B2',
    fournisseurId: 'f2',
    fournisseurNom: 'Teranga Équipements BTP',
  }),
]

describe('OffresTab (R10)', () => {
  it("n'affiche jamais le nom d'un fournisseur tant que l'AO est PUBLIE", async () => {
    server.use(http.get(`${API}/appels-offres/ao-1/offres`, () => HttpResponse.json(offresNominatives)))
    renderWithProviders(<OffresTab ao={appelOffre({ statut: 'PUBLIE' })} />, {
      user: testUser(['RESPONSABLE_ACHATS']),
    })
    expect(await screen.findByText('OFF-A1')).toBeInTheDocument()
    expect(screen.getByText('OFF-B2')).toBeInTheDocument()
    expect(screen.getByText(/anonymisées jusqu'à la clôture/)).toBeInTheDocument()
    expect(screen.queryByText(/Sahel Engins/)).not.toBeInTheDocument()
    expect(screen.queryByText(/Teranga/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Saisir une offre reçue' })).toBeEnabled()
  })

  it('leve l’anonymat apres la cloture', async () => {
    server.use(http.get(`${API}/appels-offres/ao-1/offres`, () => HttpResponse.json(offresNominatives)))
    renderWithProviders(<OffresTab ao={appelOffre({ statut: 'CLOTURE' })} />, {
      user: testUser(['RESPONSABLE_MATERIEL']),
    })
    expect(await screen.findByText('Sahel Engins SA (OFF-A1)')).toBeInTheDocument()
  })

  it('refuse la saisie apres la date limite (R02) et la masque hors RA', async () => {
    server.use(http.get(`${API}/appels-offres/ao-1/offres`, () => HttpResponse.json([])))
    const ao = appelOffre({ statut: 'PUBLIE', dateLimite: '2020-01-01T00:00:00Z' })
    const { unmount } = renderWithProviders(<OffresTab ao={ao} />, { user: testUser(['RESPONSABLE_ACHATS']) })
    expect(await screen.findByRole('button', { name: 'Saisir une offre reçue' })).toBeDisabled()
    expect(screen.getByText(/aucune offre acceptée \(R02\)/)).toBeInTheDocument()
    unmount()
    renderWithProviders(<OffresTab ao={ao} />, { user: testUser(['DIRECTION_GENERALE']) })
    expect(await screen.findByText('Aucune offre reçue')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Saisir une offre reçue' })).not.toBeInTheDocument()
  })
})
