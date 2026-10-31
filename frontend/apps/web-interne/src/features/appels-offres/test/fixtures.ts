import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest'
import { configureHttp } from '@/shared/api'
import type { AppelOffre, Attribution, BesoinAAcheter, Offre } from '../api/types'

export const API = `${window.location.origin}/api`

/** Serveur MSW du module (erreur sur toute requete non prevue). */
export function setupMsw() {
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
  return server
}

export function appelOffre(overrides: Partial<AppelOffre> = {}): AppelOffre {
  return {
    id: 'ao-1',
    reference: 'AO-2026-001',
    objet: 'Fourniture de deux pelles hydrauliques',
    type: 'APPEL_OFFRES',
    categorie: 'MATERIEL_ROULANT',
    quantite: 2,
    budgetEstime: 180_000_000,
    datePublication: '2026-10-01T09:00:00Z',
    dateLimite: '2099-01-15T17:00:00Z',
    statut: 'PUBLIE',
    criteres: [
      { code: 'PRIX', poids: 50 },
      { code: 'DELAI', poids: 20 },
      { code: 'QUALITE_TECHNIQUE', poids: 30 },
    ],
    fournisseursInvites: [
      { id: 'f0000000-0000-0000-0000-000000000001', raisonSociale: 'Sahel Engins SA' },
      { id: 'f0000000-0000-0000-0000-000000000002', raisonSociale: 'Teranga Équipements BTP' },
    ],
    nombreOffres: 2,
    creeParNom: 'Fatou Sarr',
    version: 1,
    ...overrides,
  }
}

export function offre(overrides: Partial<Offre> = {}): Offre {
  return {
    id: 'of-1',
    appelOffreId: 'ao-1',
    codeAnonyme: 'OFF-A1',
    montantPropose: 150_000_000,
    delaiLivraisonJours: 45,
    garantieMois: 12,
    dateDepot: '2026-10-10T10:00:00Z',
    notes: {},
    retenue: false,
    ...overrides,
  }
}

export function attribution(overrides: Partial<Attribution> = {}): Attribution {
  return {
    offreId: 'of-1',
    codeAnonyme: 'OFF-A1',
    fournisseurNom: 'Sahel Engins SA',
    montant: 120_000_000,
    justification: 'Meilleur score pondéré et délai conforme au planning du chantier.',
    proposeeParNom: 'Fatou Sarr',
    dateProposition: '2026-10-20T09:00:00Z',
    coValidationMateriel: { statut: 'EN_ATTENTE' },
    approbationDg: null,
    ...overrides,
  }
}

export const besoin: BesoinAAcheter = {
  besoinId: 'b-1',
  reference: 'BES-2026-0001',
  designation: 'Pelle hydraulique 20 t',
  categorie: 'MATERIEL_ROULANT',
  quantite: 2,
  priorite: 'URGENT',
  chantierId: 'c0000000-0000-0000-0000-000000000001',
  chantierNom: 'Extension autoroute Dakar-AIBD',
  dateReception: '2026-10-25T08:00:00Z',
}
