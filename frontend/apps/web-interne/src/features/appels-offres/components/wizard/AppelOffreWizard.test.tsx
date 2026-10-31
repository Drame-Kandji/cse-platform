import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders, testUser } from '@/test/render'
import { criteresSchema, sommePoids } from '../../schemas/appel-offre.schema'
import { API, besoin, setupMsw } from '../../test/fixtures'
import { AppelOffreWizard } from './AppelOffreWizard'

const server = setupMsw()

describe('ponderation des criteres', () => {
  it('exige une somme egale a 100', () => {
    expect(sommePoids([{ poids: 40 }, { poids: 60 }])).toBe(100)
    expect(criteresSchema.safeParse([{ code: 'PRIX', poids: 100 }]).success).toBe(true)
    const ko = criteresSchema.safeParse([
      { code: 'PRIX', poids: 50 },
      { code: 'DELAI', poids: 30 },
    ])
    expect(ko.success).toBe(false)
    expect(ko.error?.issues[0]?.message).toContain('actuellement 80')
  })
  it('borne chaque poids entre 0 et 100', () => {
    expect(criteresSchema.safeParse([{ code: 'PRIX', poids: 120 }]).success).toBe(false)
    expect(
      criteresSchema.safeParse([
        { code: 'PRIX', poids: -5 },
        { code: 'DELAI', poids: 105 },
      ]).success,
    ).toBe(false)
  })
})

describe('AppelOffreWizard', () => {
  it("bloque l'etape des criteres tant que la somme n'est pas 100", async () => {
    const user = userEvent.setup()
    server.use(
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
    renderWithProviders(
      <AppelOffreWizard open onOpenChange={vi.fn()} besoin={besoin} onCreated={vi.fn()} />,
      { user: testUser(['RESPONSABLE_ACHATS']) },
    )
    expect(screen.getByText('BES-2026-0001')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Suivant' }))
    await user.click(await screen.findByRole('button', { name: 'Suivant' }))
    expect(await screen.findByText('Total : 100 / 100')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Prix'), { target: { value: '30' } })
    expect(screen.getByTestId('somme-criteres')).toHaveTextContent('Total : 90 / 100')
    expect(screen.getByText('(10 à répartir)')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Suivant' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La somme des pondérations doit être égale à 100 (actuellement 90).',
    )
    expect(screen.getByText(/Étape 3 sur 6/)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Délai de livraison'), { target: { value: '30' } })
    expect(screen.getByTestId('somme-criteres')).toHaveTextContent('Total : 100 / 100')
    await user.click(screen.getByRole('button', { name: 'Suivant' }))
    await waitFor(() => {
      expect(screen.getByText(/Étape 4 sur 6/)).toBeInTheDocument()
    })
    expect(await screen.findByText('Aucun fournisseur actif pour cette catégorie')).toBeInTheDocument()
  })
})
