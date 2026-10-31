import type { StatutAO } from '@/shared/domain/labels'
import { CRITERES_MANUELS, type AppelOffre, type CritereManuel, type Offre } from '../api/types'

/**
 * Libelle d'une offre : code anonyme tant que l'AO est PUBLIE (R10), quel que soit
 * le contenu renvoye par l'API ; nom du fournisseur ensuite.
 */
export function nomOffre(offre: Pick<Offre, 'codeAnonyme' | 'fournisseurNom'>, statut: StatutAO): string {
  if (statut === 'PUBLIE') return offre.codeAnonyme
  return offre.fournisseurNom ? `${offre.fournisseurNom} (${offre.codeAnonyme})` : offre.codeAnonyme
}

/** Vrai si l'echeance de depot est depassee (R02). */
export function dateLimiteDepassee(dateLimite: string, now = Date.now()): boolean {
  return Date.parse(dateLimite) <= now
}

/** Contribution ponderee d'une note (note x poids / 100). */
export function contribution(note: number | undefined, poids: number): number | undefined {
  return note == null ? undefined : (note * poids) / 100
}

/** Criteres manuels retenus dans la ponderation de l'AO. */
export function criteresManuels(ao: AppelOffre): CritereManuel[] {
  return CRITERES_MANUELS.filter((code) => ao.criteres.some((c) => c.code === code && c.poids > 0))
}
