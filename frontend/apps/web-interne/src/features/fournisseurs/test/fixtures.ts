import { http as mswHttp, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { configureHttp } from '@/shared/api'
import type { FournisseurDetail } from '../api/types'

export const API = `${window.location.origin}/api`

export const fournisseurActif: FournisseurDetail = {
  id: 'f0000000-0000-0000-0000-000000000001',
  raisonSociale: 'Sahel Engins SA',
  ninea: '005123456 2G3',
  rccm: 'SN-DKR-2015-B-1234',
  adresse: 'Route de Rufisque',
  ville: 'Dakar',
  pays: 'Sénégal',
  telephone: '+221 33 800 00 00',
  email: 'contact@sahel-engins.sn',
  contactNom: 'Moussa Kane',
  contactFonction: 'Directeur commercial',
  specialites: ['MATERIEL_ROULANT'],
  certifications: ['ISO 9001'],
  statut: 'ACTIF',
  scorePerformance: 82.5,
  nombreEvaluations: 4,
  tauxLivraisonALHeure: 90,
  nombreLitigesOuverts: 0,
  documentsExpires: 1,
  dateInscription: '2025-02-01',
  blacklistageDemande: null,
  version: 3,
  documents: [
    { id: 'd1', type: 'QUITUS_FISCAL', libelle: 'Quitus 2025', dateExpiration: '2026-01-01', expire: true },
  ],
}

/** Serveur MSW partage par les tests du module (requete non geree = erreur). */
export function setupMsw() {
  const server = setupServer()
  beforeAll(() => {
    configureHttp({ baseUrl: '/api', getToken: () => Promise.resolve(null) })
    server.listen({ onUnhandledRequest: 'error' })
  })
  afterEach(() => {
    server.resetHandlers()
  })
  afterAll(() => {
    server.close()
  })
  return server
}

export { mswHttp, HttpResponse }
