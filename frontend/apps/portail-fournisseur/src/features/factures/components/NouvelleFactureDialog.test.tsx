import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { contrat, facture } from '@/test/fixtures'
import { renderPortail } from '@/test/render'
import { api, http, HttpResponse, problem, server } from '@/test/server'
import { NouvelleFactureDialog } from './NouvelleFactureDialog'

const livre = contrat({ statut: 'LIVRE' })

describe('NouvelleFactureDialog', () => {
  it('calcule la TVA et le TTC en direct et envoie le montant de TVA', async () => {
    let corps: Record<string, unknown> = {}
    server.use(
      http.post(api('/factures'), async ({ request }) => {
        corps = (await request.json()) as Record<string, unknown>
        return HttpResponse.json(facture(), { status: 201 })
      }),
    )
    const onOpenChange = vi.fn()
    const user = userEvent.setup()
    renderPortail(
      <NouvelleFactureDialog open onOpenChange={onOpenChange} contrats={[livre]} contratId={livre.id} />,
    )
    expect(screen.getByLabelText(/^TVA/)).toHaveValue('18')
    await user.type(screen.getByLabelText(/Numéro de facture/), 'FA-2026-001')
    await user.type(screen.getByLabelText(/Montant HT/), '1000000')
    expect(screen.getByTestId('ttc')).toHaveTextContent(/1\s180\s000\sFCFA/)
    await user.clear(screen.getByLabelText(/^TVA/))
    await user.type(screen.getByLabelText(/^TVA/), '10')
    expect(screen.getByTestId('ttc')).toHaveTextContent(/1\s100\s000\sFCFA/)

    await user.click(screen.getByRole('button', { name: 'Déposer la facture' }))
    await waitFor(() => {
      expect(onOpenChange).toHaveBeenCalledWith(false)
    })
    expect(corps).toMatchObject({
      contratId: 'ct-1',
      numero: 'FA-2026-001',
      montantHt: 1_000_000,
      tva: 100_000,
    })
    expect(corps.dateEmission).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it('FACTURE_EXISTANTE : erreur sur le numero', async () => {
    server.use(http.post(api('/factures'), () => problem(409, 'FACTURE_EXISTANTE')))
    const user = userEvent.setup()
    renderPortail(
      <NouvelleFactureDialog open onOpenChange={vi.fn()} contrats={[livre]} contratId={livre.id} />,
    )
    await user.type(screen.getByLabelText(/Numéro de facture/), 'FA-1')
    await user.type(screen.getByLabelText(/Montant HT/), '500')
    await user.click(screen.getByRole('button', { name: 'Déposer la facture' }))
    expect(await screen.findByText('Une facture avec ce numéro existe déjà.')).toBeInTheDocument()
  })

  it('aucun contrat livre : depot impossible', () => {
    renderPortail(<NouvelleFactureDialog open onOpenChange={vi.fn()} contrats={[]} />)
    expect(screen.getByRole('button', { name: 'Déposer la facture' })).toBeDisabled()
  })
})
