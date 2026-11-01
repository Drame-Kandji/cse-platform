import { toDate } from '@/shared/lib/format'
import type { AppelOffrePortail, OffrePortail, ResultatOffre } from './types'

/**
 * Vrai si le fournisseur est invite. Le champ `invite` (§13) peut manquer : une demande de prix
 * n'est visible que des invites, une procedure ouverte ne l'est pas forcement.
 */
export function estInvite(ao: Pick<AppelOffrePortail, 'invite' | 'type'>): boolean {
  return ao.invite ?? ao.type === 'DEMANDE_PRIX'
}

/**
 * Resultat d'une offre : champ `resultat` (§13) ou, a defaut, deduit de l'appel d'offres
 * (RETENUE / NON_RETENUE seulement une fois l'AO ATTRIBUE).
 */
export function resultatOffre(
  offre: Pick<OffrePortail, 'resultat' | 'statutAppelOffre' | 'retenue'>,
): ResultatOffre {
  if (offre.resultat) return offre.resultat
  if (offre.statutAppelOffre !== 'ATTRIBUE') return 'EN_COURS'
  return offre.retenue ? 'RETENUE' : 'NON_RETENUE'
}

const MINUTE = 60_000
const HEURE = 60 * MINUTE
const JOUR = 24 * HEURE

export interface Decompte {
  readonly label: string
  /** Moins de 24 h restantes (affiche en rouge). */
  readonly urgent: boolean
  readonly expire: boolean
}

/**
 * Temps restant avant la date limite de depot.
 * @example decompte('2026-11-03T17:00:00Z', Date.parse('2026-11-01T12:00:00Z')) // { label: '2 j 5 h', ... }
 */
export function decompte(dateLimite: string, now: number): Decompte {
  const reste = (toDate(dateLimite)?.getTime() ?? now) - now
  if (reste <= 0) return { label: 'Clôturé', urgent: false, expire: true }
  const jours = Math.floor(reste / JOUR)
  const heures = Math.floor((reste % JOUR) / HEURE)
  const minutes = Math.floor((reste % HEURE) / MINUTE)
  const label =
    jours > 0
      ? `${jours} j ${heures} h`
      : heures > 0
        ? `${heures} h ${String(minutes).padStart(2, '0')}`
        : `${Math.max(minutes, 1)} min`
  return { label, urgent: reste < JOUR, expire: false }
}

/** Vrai si une offre peut encore etre deposee sur cet appel d'offres. */
export function peutDeposer(ao: AppelOffrePortail, now: number): boolean {
  return ao.statut === 'PUBLIE' && !ao.offreDeposee && !decompte(ao.dateLimite, now).expire
}
