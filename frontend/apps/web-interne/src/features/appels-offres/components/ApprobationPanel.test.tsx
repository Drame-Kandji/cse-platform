import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { Role } from '@/shared/domain/labels'
import { renderWithProviders, testUser } from '@/test/render'
import type { Attribution } from '../api/types'
import { API, appelOffre, attribution, setupMsw } from '../test/fixtures'
import { ApprobationPanel } from './ApprobationPanel'

const server = setupMsw()

function rendre(roles: Role[], attr: Attribution) {
  const ao = appelOffre({ statut: 'CLOTURE', attribution: attr })
  return renderWithProviders(<ApprobationPanel ao={ao} attribution={attr} />, { user: testUser(roles) })
}

const auDessusDuSeuil = attribution({
  montant: 200_000_000,
  approbationDg: { statut: 'EN_ATTENTE' },
})

describe('ApprobationPanel (R11)', () => {
  it('le RM co-valide ; aucune action DG pour lui', () => {
    rendre(['RESPONSABLE_MATERIEL'], auDessusDuSeuil)
    expect(screen.getByRole('button', { name: 'Co-valider' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approuver' })).not.toBeInTheDocument()
  })

  it('le DG approuve uniquement quand son approbation est requise', () => {
    const { unmount } = rendre(['DIRECTION_GENERALE'], auDessusDuSeuil)
    expect(screen.getByRole('button', { name: 'Approuver' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Co-valider' })).not.toBeInTheDocument()
    expect(screen.getByText(/supérieur au seuil de délégation/)).toBeInTheDocument()
    unmount()

    rendre(['DIRECTION_GENERALE'], attribution({ approbationDg: null }))
    expect(screen.queryByRole('button', { name: 'Approuver' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Refuser' })).not.toBeInTheDocument()
    expect(screen.getByText(/sans approbation de la Direction Générale/)).toBeInTheDocument()
  })

  it("le RA et le DAF n'ont aucune action", () => {
    const { unmount } = rendre(['RESPONSABLE_ACHATS'], auDessusDuSeuil)
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
    unmount()
    rendre(['DAF'], auDessusDuSeuil)
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('masque les actions une fois la decision prise', () => {
    rendre(
      ['RESPONSABLE_MATERIEL'],
      attribution({
        coValidationMateriel: { statut: 'APPROUVEE', parNom: 'Ibrahima Fall', date: '2026-10-21T10:00:00Z' },
      }),
    )
    expect(screen.queryByRole('button', { name: 'Co-valider' })).not.toBeInTheDocument()
    expect(screen.getByText(/Ibrahima Fall/)).toBeInTheDocument()
  })

  it('exige un commentaire pour refuser puis envoie la decision', async () => {
    const user = userEvent.setup()
    let corps: unknown
    server.use(
      http.post(`${API}/appels-offres/ao-1/attribution/co-valider`, async ({ request }) => {
        corps = await request.json()
        return HttpResponse.json(appelOffre({ statut: 'CLOTURE', attribution: null }))
      }),
    )
    rendre(['RESPONSABLE_MATERIEL'], auDessusDuSeuil)
    await user.click(screen.getByRole('button', { name: 'Refuser' }))
    await user.click(await screen.findByRole('button', { name: 'Confirmer le refus' }))
    expect(await screen.findByText('Motivez le refus (5 caractères minimum).')).toBeInTheDocument()
    await user.type(screen.getByLabelText(/Commentaire/), 'Spécifications non conformes')
    await user.click(screen.getByRole('button', { name: 'Confirmer le refus' }))
    await waitFor(() => {
      expect(corps).toEqual({ approuve: false, commentaire: 'Spécifications non conformes' })
    })
  })
})
