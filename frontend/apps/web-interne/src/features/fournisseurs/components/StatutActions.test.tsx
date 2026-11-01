import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { toast } from 'sonner'
import { describe, expect, it, vi } from 'vitest'
import type { Role } from '@/shared/domain/labels'
import { renderWithProviders, testUser } from '@/test/render'
import type { FournisseurDetail } from '../api/types'
import { API, fournisseurActif, HttpResponse, mswHttp, setupMsw } from '../test/fixtures'
import { StatutActions } from './StatutActions'

const demande: FournisseurDetail = {
  ...fournisseurActif,
  statut: 'SUSPENDU',
  blacklistageDemande: { demandeurNom: 'Fatou Sarr', motif: 'Fraude documentaire', date: '2026-10-01' },
}

const server = setupMsw()

function renderFor(roles: Role[], fournisseur: FournisseurDetail) {
  return renderWithProviders(<StatutActions fournisseur={fournisseur} />, { user: testUser(roles) })
}

describe('StatutActions', () => {
  it('le RA doit suspendre avant de demander le blacklistage (ACTIF)', () => {
    renderFor(['RESPONSABLE_ACHATS'], fournisseurActif)
    expect(screen.getByRole('button', { name: 'Suspendre' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Demander le blacklistage' })).toBeDisabled()
    expect(screen.getByText(/Suspendre d'abord le fournisseur/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approuver le blacklistage' })).not.toBeInTheDocument()
  })

  it('le RA demande le blacklistage d un fournisseur SUSPENDU', () => {
    renderFor(['RESPONSABLE_ACHATS'], { ...fournisseurActif, statut: 'SUSPENDU' })
    expect(screen.getByRole('button', { name: 'Demander le blacklistage' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Réactiver' })).toBeInTheDocument()
    expect(screen.queryByText(/Suspendre d'abord/)).not.toBeInTheDocument()
  })

  it('le RA ne redemande pas un blacklistage deja en attente', () => {
    renderFor(['RESPONSABLE_ACHATS'], demande)
    expect(screen.queryByRole('button', { name: 'Demander le blacklistage' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approuver le blacklistage' })).not.toBeInTheDocument()
  })

  it('la DG approuve ou refuse uniquement une demande en attente (R09)', () => {
    const { unmount } = renderFor(['DIRECTION_GENERALE'], fournisseurActif)
    expect(screen.queryByRole('button', { name: 'Approuver le blacklistage' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Suspendre' })).not.toBeInTheDocument()
    unmount()
    renderFor(['DIRECTION_GENERALE'], demande)
    expect(screen.getByRole('button', { name: 'Approuver le blacklistage' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Refuser le blacklistage' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Demander le blacklistage' })).not.toBeInTheDocument()
  })

  it('le RA valide une inscription en attente', () => {
    renderFor(['RESPONSABLE_ACHATS'], { ...fournisseurActif, statut: 'EN_ATTENTE_VALIDATION' })
    expect(screen.getByRole('button', { name: "Valider l'inscription" })).toBeInTheDocument()
  })

  it('le RA valide l inscription et est informe de l e-mail envoye au fournisseur', async () => {
    const user = userEvent.setup()
    const success = vi.spyOn(toast, 'success')
    let called = false
    const enAttente: FournisseurDetail = { ...fournisseurActif, statut: 'EN_ATTENTE_VALIDATION' }
    server.use(
      mswHttp.patch(`${API}/fournisseurs/${enAttente.id}/valider-inscription`, () => {
        called = true
        return HttpResponse.json({ ...enAttente, statut: 'ACTIF' })
      }),
    )
    renderFor(['RESPONSABLE_ACHATS'], enAttente)
    await user.click(screen.getByRole('button', { name: "Valider l'inscription" }))
    const dialog = await screen.findByRole('alertdialog')
    expect(within(dialog).getByText(/e-mail envoyé pour définir le mot de passe/)).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: "Valider l'inscription" }))
    await waitFor(() => {
      expect(called).toBe(true)
    })
    await waitFor(() => {
      expect(success).toHaveBeenCalledWith(expect.stringMatching(/e-mail a été envoyé au fournisseur/))
    })
    success.mockRestore()
  })

  it('la DG ne peut pas approuver sa propre demande', () => {
    renderFor(['DIRECTION_GENERALE'], {
      ...demande,
      blacklistageDemande: { demandeurNom: 'Test Utilisateur', motif: 'Fraude', date: '2026-10-01' },
    })
    expect(screen.getByRole('button', { name: 'Approuver le blacklistage' })).toBeDisabled()
    expect(screen.getByText(/Vous êtes l'auteur de cette demande/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Refuser le blacklistage' })).toBeEnabled()
  })
})
