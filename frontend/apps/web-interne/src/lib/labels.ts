import type {
  EtatMateriel,
  Priorite,
  StatutAppelOffre,
  StatutBesoin,
  StatutFournisseur,
} from '../api/types'

export type Tone = 'brand' | 'accent' | 'success' | 'danger' | 'warning' | 'neutral' | 'info'

export type Role =
  | 'CHEF_CHANTIER'
  | 'RESPONSABLE_MATERIEL'
  | 'RESPONSABLE_ACHATS'
  | 'DAF'
  | 'DIRECTION_GENERALE'
  | 'ADMIN_SYSTEME'

export const ROLE_LABELS: Record<Role, string> = {
  CHEF_CHANTIER: 'Chef de chantier',
  RESPONSABLE_MATERIEL: 'Responsable matériel',
  RESPONSABLE_ACHATS: 'Responsable achats',
  DAF: 'Directeur financier (DAF)',
  DIRECTION_GENERALE: 'Direction générale',
  ADMIN_SYSTEME: 'Administrateur système',
}

export const ROLE_SHORT: Record<Role, string> = {
  CHEF_CHANTIER: 'Chantier',
  RESPONSABLE_MATERIEL: 'Parc',
  RESPONSABLE_ACHATS: 'Achats',
  DAF: 'DAF',
  DIRECTION_GENERALE: 'DG',
  ADMIN_SYSTEME: 'Admin',
}

export const ROLE_DESCRIPTIONS: Record<Role, string> = {
  CHEF_CHANTIER: 'Exprime les besoins du chantier et suit les affectations.',
  RESPONSABLE_MATERIEL: 'Valide les besoins, affecte les engins et pilote la maintenance.',
  RESPONSABLE_ACHATS: 'Publie les appels d’offres et gère la relation fournisseurs.',
  DAF: 'Valide les engagements budgétaires et mandate les paiements.',
  DIRECTION_GENERALE: 'Arbitre les réformes et les blacklistages, supervise les KPIs.',
  ADMIN_SYSTEME: 'Gère les comptes, les habilitations et le journal d’audit.',
}

export const ROLE_TONE: Record<Role, Tone> = {
  CHEF_CHANTIER: 'info',
  RESPONSABLE_MATERIEL: 'brand',
  RESPONSABLE_ACHATS: 'accent',
  DAF: 'success',
  DIRECTION_GENERALE: 'brand',
  ADMIN_SYSTEME: 'neutral',
}

export const STATUT_BESOIN: Record<StatutBesoin, { label: string; tone: Tone }> = {
  EN_ATTENTE: { label: 'En attente', tone: 'warning' },
  VALIDE: { label: 'Validé', tone: 'success' },
  REJETE: { label: 'Rejeté', tone: 'danger' },
  EN_ACHAT: { label: 'En achat', tone: 'info' },
  SATISFAIT: { label: 'Satisfait', tone: 'neutral' },
}

export const PRIORITE: Record<Priorite, { label: string; tone: Tone }> = {
  NORMAL: { label: 'Normal', tone: 'neutral' },
  URGENT: { label: 'Urgent', tone: 'warning' },
  CRITIQUE: { label: 'Critique', tone: 'danger' },
}

export const ETAT_MATERIEL: Record<EtatMateriel, { label: string; tone: Tone; dot: string }> = {
  DISPONIBLE: { label: 'Disponible', tone: 'success', dot: 'bg-success-500' },
  AFFECTE: { label: 'Affecté', tone: 'info', dot: 'bg-brand-500' },
  EN_MAINTENANCE: { label: 'En maintenance', tone: 'warning', dot: 'bg-accent-500' },
  EN_PANNE: { label: 'En panne', tone: 'danger', dot: 'bg-danger-500' },
  REFORME: { label: 'Réformé', tone: 'neutral', dot: 'bg-ink-400' },
}

export const STATUT_AO: Record<StatutAppelOffre, { label: string; tone: Tone }> = {
  PUBLIE: { label: 'Publié', tone: 'info' },
  CLOTURE: { label: 'Clôturé', tone: 'warning' },
  ATTRIBUE: { label: 'Attribué', tone: 'success' },
}

export const STATUT_FOURNISSEUR: Record<StatutFournisseur, { label: string; tone: Tone }> = {
  ACTIF: { label: 'Actif', tone: 'success' },
  EN_ATTENTE_VALIDATION: { label: 'En attente de validation', tone: 'warning' },
  SUSPENDU: { label: 'Suspendu', tone: 'danger' },
  BLACKLISTE: { label: 'Blacklisté', tone: 'neutral' },
}

export const SPECIALITES: { value: string; label: string }[] = [
  { value: 'ENGINS_LOURDS', label: 'Engins lourds' },
  { value: 'MATERIEL_ROULANT', label: 'Matériel roulant' },
  { value: 'LEVAGE_GRUES', label: 'Levage & grues' },
  { value: 'TRANSPORT_BENNES', label: 'Transport & bennes' },
  { value: 'MAINTENANCE_PIECES', label: 'Maintenance & pièces' },
  { value: 'MATERIAUX_CONSTRUCTION', label: 'Matériaux de construction' },
  { value: 'SERVICES', label: 'Services' },
]

export const CATEGORIES_MATERIEL: { value: string; label: string }[] = [
  { value: 'PELLE', label: 'Pelle' },
  { value: 'BULLDOZER', label: 'Bulldozer' },
  { value: 'NIVELEUSE', label: 'Niveleuse' },
  { value: 'CAMION_BENNE', label: 'Camion-benne' },
  { value: 'GRUE', label: 'Grue' },
  { value: 'CHARGEUSE', label: 'Chargeuse' },
  { value: 'COMPACTEUR', label: 'Compacteur' },
]

export const MAINTENANCE_TYPES: { value: 'PREVENTIVE' | 'CORRECTIVE' | 'LOURDE'; label: string }[] = [
  { value: 'PREVENTIVE', label: 'Préventive' },
  { value: 'CORRECTIVE', label: 'Corrective' },
  { value: 'LOURDE', label: 'Lourde' },
]

export const URGENCES_PANNE: { value: 'NORMALE' | 'URGENTE' | 'CRITIQUE'; label: string }[] = [
  { value: 'NORMALE', label: 'Normale' },
  { value: 'URGENTE', label: 'Urgente' },
  { value: 'CRITIQUE', label: 'Critique' },
]

export const USERS_ROLES: Role[] = [
  'CHEF_CHANTIER',
  'RESPONSABLE_MATERIEL',
  'RESPONSABLE_ACHATS',
  'DAF',
  'DIRECTION_GENERALE',
  'ADMIN_SYSTEME',
]

export function labelOfSpecialite(value: string): string {
  return SPECIALITES.find((item) => item.value === value)?.label ?? value
}

export function labelOfCategorie(value: string): string {
  return CATEGORIES_MATERIEL.find((item) => item.value === value)?.label ?? value
}

export function toneForScore(score: number): Tone {
  if (score < 50) return 'danger'
  if (score < 70) return 'warning'
  if (score < 85) return 'info'
  return 'success'
}

export function toneForStatutFacture(statut: string): Tone {
  if (statut === 'PAIEMENT_MANDATE' || statut === 'PAYEE') return 'success'
  if (statut === 'EN_ATTENTE_MANDATEMENT') return 'warning'
  return 'neutral'
}

export function toneForStatutContrat(statut: string): Tone {
  if (statut === 'ENGAGEMENT_VALIDE' || statut === 'SIGNE') return 'success'
  if (statut === 'EN_ATTENTE') return 'warning'
  return 'neutral'
}

export function labelStatut(statut: string): string {
  return statut
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}
