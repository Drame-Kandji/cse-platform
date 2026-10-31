import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import type { Role } from '@/shared/domain/labels'
import { renderWithProviders, testUser } from '@/test/render'
import type { FournisseurDetail } from '../api/types'
import { API, fournisseurActif, HttpResponse, mswHttp, setupMsw } from '../test/fixtures'
import { Component as FournisseurDetailPage } from './FournisseurDetailPage'

const server = setupMsw()

function renderDetail(roles: Role[], detail: FournisseurDetail) {
  server.use(mswHttp.get(`${API}/fournisseurs/${detail.id}`, () => HttpResponse.json(detail)))
  return renderWithProviders(
    <Routes>
      <Route path="/fournisseurs/:id" element={<FournisseurDetailPage />} />
    </Routes>,
    { user: testUser(roles), route: `/fournisseurs/${detail.id}` },
  )
}

describe('FournisseurDetailPage', () => {
  it('signale les documents expires et masque la banque si non renvoyee', async () => {
    renderDetail(['RESPONSABLE_MATERIEL'], fournisseurActif)
    expect(await screen.findByRole('heading', { name: 'Sahel Engins SA' })).toBeInTheDocument()
    expect(screen.getByText('Expiré')).toBeInTheDocument()
    expect(screen.queryByText('Coordonnées bancaires')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Modifier' })).not.toBeInTheDocument()
  })

  it('affiche les coordonnees bancaires renvoyees et la demande de blacklistage en attente', async () => {
    renderDetail(['RESPONSABLE_ACHATS'], {
      ...fournisseurActif,
      coordonneesBancaires: { banque: 'CBAO', iban: 'SN08 SN01 0000 0000 0000' },
      blacklistageDemande: { demandeurNom: 'Fatou Sarr', motif: 'Fraude', date: '2026-10-01' },
    })
    expect(await screen.findByText('Coordonnées bancaires')).toBeInTheDocument()
    expect(screen.getByText('SN08 SN01 0000 0000 0000')).toBeInTheDocument()
    expect(screen.getByText(/en attente de la Direction Générale/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Modifier' })).toBeInTheDocument()
  })
})
