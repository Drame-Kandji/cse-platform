import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, testUser } from '@/test/render'
import { API, fournisseurActif, HttpResponse, mswHttp, setupMsw } from '../test/fixtures'
import { EvaluationsTab } from './EvaluationsTab'

const server = setupMsw()
const id = fournisseurActif.id

describe('EvaluationsTab', () => {
  it("masque Evaluer tant que l'inscription n'est pas validee", async () => {
    server.use(mswHttp.get(`${API}/fournisseurs/${id}/evaluations`, () => HttpResponse.json([])))
    renderWithProviders(<EvaluationsTab fournisseurId={id} statut="EN_ATTENTE_VALIDATION" />, {
      user: testUser(['RESPONSABLE_ACHATS']),
    })
    expect(await screen.findByText('Aucune évaluation')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Évaluer' })).not.toBeInTheDocument()
    expect(screen.getByText(/après validation de l'inscription/)).toBeInTheDocument()
  })

  it('propose Evaluer pour un fournisseur actif', async () => {
    server.use(mswHttp.get(`${API}/fournisseurs/${id}/evaluations`, () => HttpResponse.json([])))
    renderWithProviders(<EvaluationsTab fournisseurId={id} statut="ACTIF" />, {
      user: testUser(['RESPONSABLE_MATERIEL']),
    })
    expect(await screen.findByRole('button', { name: 'Évaluer' })).toBeInTheDocument()
  })
})
