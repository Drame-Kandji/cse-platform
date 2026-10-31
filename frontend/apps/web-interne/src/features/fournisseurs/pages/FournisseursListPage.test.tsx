import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, testUser } from '@/test/render'
import { API, fournisseurActif, HttpResponse, mswHttp, setupMsw } from '../test/fixtures'
import { Component as FournisseursListPage } from './FournisseursListPage'

const server = setupMsw()

describe('FournisseursListPage', () => {
  it('affiche les fournisseurs, leur statut et le bouton de creation pour le RA', async () => {
    let query: URLSearchParams | undefined
    server.use(
      mswHttp.get(`${API}/fournisseurs`, ({ request }) => {
        query = new URL(request.url).searchParams
        return HttpResponse.json({
          content: [fournisseurActif],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        })
      }),
      mswHttp.get(`${API}/fournisseurs/statistiques`, () =>
        HttpResponse.json({
          total: 7,
          actifs: 5,
          enAttente: 1,
          suspendus: 1,
          blacklistes: 0,
          scoreMoyen: 71.2,
          litigesOuverts: 2,
          documentsExpires: 3,
        }),
      ),
    )
    renderWithProviders(<FournisseursListPage />, {
      user: testUser(['RESPONSABLE_ACHATS']),
      route: '/fournisseurs?statut=ACTIF',
    })
    expect(await screen.findByText('Sahel Engins SA')).toBeInTheDocument()
    expect(screen.getAllByText('Actif').length).toBeGreaterThan(0)
    expect(screen.getByText('Matériel roulant')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nouveau fournisseur' })).toBeInTheDocument()
    expect(query?.get('statut')).toBe('ACTIF')
    expect(query?.get('sort')).toBe('raisonSociale,asc')
  })

  it('masque la creation pour la DG', async () => {
    server.use(
      mswHttp.get(`${API}/fournisseurs`, () =>
        HttpResponse.json({
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
        }),
      ),
      mswHttp.get(`${API}/fournisseurs/statistiques`, () => HttpResponse.json({}, { status: 500 })),
    )
    renderWithProviders(<FournisseursListPage />, {
      user: testUser(['DIRECTION_GENERALE']),
      route: '/fournisseurs',
    })
    expect(await screen.findByText('Aucun fournisseur')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Nouveau fournisseur' })).not.toBeInTheDocument()
  })
})
