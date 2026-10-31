import type { PageResponse } from '@/shared/api'
import type { Chantier } from '@/shared/domain/types'
import type { Besoin } from '../api/types'

/** URL absolue d'un endpoint pour les handlers MSW. */
export const api = (path: string) => `${window.location.origin}/api${path}`

/** Page de resultats de test. */
export function page<T>(content: T[], totalElements = content.length): PageResponse<T> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements,
    totalPages: Math.max(1, Math.ceil(totalElements / 20)),
    first: true,
    last: true,
  }
}

/** Besoin de demonstration (contrat §12 : chantier 1, chef Mamadou Diop). */
export function besoinFixture(overrides: Partial<Besoin> = {}): Besoin {
  return {
    id: 'b0000000-0000-0000-0000-000000000001',
    reference: 'BES-2026-0001',
    designation: 'Pelle hydraulique 20 t',
    categorie: 'MATERIEL_ROULANT',
    quantite: 2,
    specifications: 'Godet 1,2 m3',
    priorite: 'URGENT',
    statut: 'EN_ATTENTE',
    chantierId: 'c0000000-0000-0000-0000-000000000001',
    chantierNom: 'Extension autoroute Dakar–AIBD',
    demandeurId: '00000000-0000-0000-0000-000000000101',
    demandeurNom: 'Mamadou Diop',
    dureeUtilisationJours: 30,
    dateBesoin: '2026-11-15',
    justification: 'Terrassement à l’arrêt',
    materielIdsAffectes: [],
    orientationEnAttente: false,
    createdAt: '2026-10-28T09:15:00Z',
    updatedAt: '2026-10-28T09:15:00Z',
    version: 0,
    ...overrides,
  }
}

/** Chantier de demonstration. */
export function chantierFixture(overrides: Partial<Chantier> = {}): Chantier {
  return {
    id: 'c0000000-0000-0000-0000-000000000001',
    code: 'CH-DKR-01',
    nom: 'Extension autoroute Dakar–AIBD',
    localisation: 'Diamniadio',
    region: 'Dakar',
    latitude: 14.7167,
    longitude: -17.4677,
    statut: 'EN_COURS',
    budget: 4_500_000_000,
    montantMarche: 5_200_000_000,
    dateDebut: '2026-02-01',
    dateFinPrevue: '2027-06-30',
    chefChantierId: '00000000-0000-0000-0000-000000000101',
    chefChantierNom: 'Mamadou Diop',
    nombreMateriels: 12,
    version: 1,
    ...overrides,
  }
}
