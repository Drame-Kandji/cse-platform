import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { renderWithProviders, testUser } from '@/test/render'
import { profilsAutorises, resolveProfil } from '../lib/profils'
import type { Tableau } from '../types'
import { Component as DashboardPage } from './DashboardPage'

const API = `${window.location.origin}/api`
const server = setupServer()
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => {
  server.resetHandlers()
})
afterAll(() => {
  server.close()
})

const emptyPage = { content: [], page: 0, size: 1, totalElements: 3, totalPages: 3, first: true, last: false }

const tableauRm: Tableau = {
  profil: 'RESPONSABLE_MATERIEL',
  du: '2026-08-01',
  au: '2026-10-30',
  genereLe: '2026-10-30T08:00:00Z',
  parc: {
    total: 120,
    disponibles: 60,
    affectes: 50,
    enMaintenance: 8,
    reformes: 2,
    operationnels: 110,
    tauxDisponibilite: 50.8,
    tauxUtilisation: 71.2,
    maintenancesEnCours: 4,
    pannesOuvertes: 3,
    alertesMaintenance: 5,
    parCategorie: [
      {
        categorie: 'MATERIEL_ROULANT',
        total: 80,
        disponibles: 40,
        affectes: 35,
        enMaintenance: 5,
        tauxUtilisation: 70,
      },
    ],
  },
  besoins: {
    total: 42,
    enAttente: 7,
    valides: 20,
    rejetes: 3,
    affectesDirect: 8,
    enAchat: 4,
    delaiMoyenTraitementHeures: 18.5,
    parPriorite: { NORMAL: 30, URGENT: 9, CRITIQUE: 3 },
  },
  chantiers: [
    {
      chantierId: 'c1',
      chantierNom: 'Extension autoroute Dakar–AIBD',
      region: 'Dakar',
      budget: 900_000_000,
      montantMarche: 1_200_000_000,
      montantEngage: 400_000_000,
      coutExploitation: 50_000_000,
      coutMaintenance: 10_000_000,
      marge: 740_000_000,
      materielsAffectes: 18,
      tauxUtilisation: 82,
      besoinsEnAttente: 2,
    },
  ],
}

function installHandlers(seen: { kpis: URLSearchParams[]; series: string[] }, tableau: Tableau) {
  server.use(
    http.get(`${API}/reporting/kpis`, ({ request }) => {
      seen.kpis.push(new URL(request.url).searchParams)
      return HttpResponse.json(tableau)
    }),
    http.get(`${API}/reporting/series`, ({ request }) => {
      seen.series.push(new URL(request.url).searchParams.get('indicateur') ?? '')
      return HttpResponse.json([
        { periode: '2026-08', valeur: 60 },
        { periode: '2026-09', valeur: 65 },
      ])
    }),
    http.get(`${API}/besoins/statistiques`, () =>
      HttpResponse.json({ total: 42, parStatut: { EN_ATTENTE: 7 }, parPriorite: {} }),
    ),
    http.get(`${API}/*`, () => HttpResponse.json(emptyPage)),
  )
}

describe('profils autorises', () => {
  it('limite chaque role a son profil, la DG a tous', () => {
    expect(profilsAutorises(['RESPONSABLE_MATERIEL'])).toEqual(['RESPONSABLE_MATERIEL'])
    expect(profilsAutorises(['DIRECTION_GENERALE'])).toEqual([
      'DG',
      'DAF',
      'RESPONSABLE_ACHATS',
      'RESPONSABLE_MATERIEL',
      'CHEF_CHANTIER',
    ])
    expect(profilsAutorises(['ADMIN_SYSTEME'])).toEqual([])
    expect(resolveProfil('DAF', ['RESPONSABLE_MATERIEL'])).toBe('RESPONSABLE_MATERIEL')
  })
})

describe('DashboardPage', () => {
  it('reste sobre : 4 KPI du profil, un seul graphique et le lien vers l\'analyse detaillee', async () => {
    const seen = { kpis: [] as URLSearchParams[], series: [] as string[] }
    installHandlers(seen, tableauRm)
    const { container } = renderWithProviders(<DashboardPage />, { user: testUser(['RESPONSABLE_MATERIEL']) })
    expect(await screen.findByText('Disponibilité opérationnelle')).toBeInTheDocument()
    expect(screen.getByText('110 opérationnels sur 118')).toBeInTheDocument()
    expect(screen.getByRole('list', { name: 'Indicateurs clés' }).children).toHaveLength(4)
    expect(container.querySelector('[data-section="parc"]')).not.toBeNull()
    // Graphiques secondaires et tableau des chantiers : deplaces dans Rapports.
    expect(container.querySelector('[data-section="besoins"]')).toBeNull()
    expect(container.querySelector('[data-section="chantiers"]')).toBeNull()
    expect(container.querySelector('[data-section="finances"]')).toBeNull()
    expect(container.querySelector('[data-section="fournisseurs"]')).toBeNull()
    expect(container.querySelector('[data-section="achats"]')).toBeNull()
    expect(screen.queryByText('Montant engagé')).not.toBeInTheDocument()
    expect(seen.kpis[0]?.get('profil')).toBe('RESPONSABLE_MATERIEL')
    await waitFor(() => {
      expect(new Set(seen.series)).toEqual(new Set(['TAUX_DISPONIBILITE', 'TAUX_UTILISATION']))
    })
    expect(screen.getByText('Besoins en attente de validation')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: "Voir l'analyse détaillée" })).toBeInTheDocument()
    // Un seul profil autorise : pas de selecteur de vue.
    expect(screen.queryByRole('combobox', { name: 'Vue du tableau de bord' })).not.toBeInTheDocument()
  })

  it("ignore une vue non autorisee demandee dans l'URL", async () => {
    const seen = { kpis: [] as URLSearchParams[], series: [] as string[] }
    installHandlers(seen, tableauRm)
    renderWithProviders(<DashboardPage />, { user: testUser(['RESPONSABLE_MATERIEL']), route: '/?vue=DG' })
    await screen.findByText('Disponibilité opérationnelle')
    expect(seen.kpis[0]?.get('profil')).toBe('RESPONSABLE_MATERIEL')
  })

  it('propose a la DG le choix de la vue et applique la vue demandee', async () => {
    const seen = { kpis: [] as URLSearchParams[], series: [] as string[] }
    installHandlers(seen, { ...tableauRm, profil: 'DAF', parc: null, besoins: null })
    renderWithProviders(<DashboardPage />, { user: testUser(['DIRECTION_GENERALE']), route: '/?vue=DAF' })
    expect(await screen.findByRole('combobox', { name: 'Vue du tableau de bord' })).toHaveTextContent(
      'Vue Direction financière',
    )
    await waitFor(() => {
      expect(seen.kpis[0]?.get('profil')).toBe('DAF')
    })
  })

  it("affiche les acces d'administration pour un administrateur (aucun profil)", () => {
    renderWithProviders(<DashboardPage />, { user: testUser(['ADMIN_SYSTEME']) })
    expect(screen.getByText('État de la plateforme')).toBeInTheDocument()
  })
})
