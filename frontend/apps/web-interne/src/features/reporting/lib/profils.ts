import type { Role } from '@/shared/domain/labels'
import { PROFILS, type Profil } from '../types'

/** Libelles et accroches des profils de tableau de bord. */
export const PROFIL_LABELS: Readonly<Record<Profil, { label: string; vue: string }>> = {
  DG: { label: 'Direction Générale', vue: 'Vue stratégique consolidée' },
  DAF: { label: 'Direction financière', vue: 'Vue financière' },
  RESPONSABLE_ACHATS: { label: 'Achats', vue: 'Vue approvisionnement' },
  RESPONSABLE_MATERIEL: { label: 'Matériel', vue: 'Vue opérationnelle du parc' },
  CHEF_CHANTIER: { label: 'Chantier', vue: 'Vue de vos chantiers' },
}

const PROFIL_BY_ROLE: Readonly<Partial<Record<Role, Profil>>> = {
  DIRECTION_GENERALE: 'DG',
  DAF: 'DAF',
  RESPONSABLE_ACHATS: 'RESPONSABLE_ACHATS',
  RESPONSABLE_MATERIEL: 'RESPONSABLE_MATERIEL',
  CHEF_CHANTIER: 'CHEF_CHANTIER',
}

/**
 * Profils de tableau de bord autorises (contrat §6) : la Direction Generale voit tous les
 * profils, les autres roles leur seul profil ; ordre = priorite du profil principal.
 * @returns liste vide si aucun profil (ex. administrateur systeme)
 */
export function profilsAutorises(roles: readonly Role[]): Profil[] {
  if (roles.includes('DIRECTION_GENERALE')) return [...PROFILS]
  const owned = new Set(roles.map((role) => PROFIL_BY_ROLE[role]).filter((p) => p !== undefined))
  return PROFILS.filter((profil) => owned.has(profil))
}

/** Valide un profil demande (URL) contre les profils autorises, sinon profil principal. */
export function resolveProfil(requested: string | null, allowed: readonly Profil[]): Profil | undefined {
  return allowed.find((profil) => profil === requested) ?? allowed[0]
}
