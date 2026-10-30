import type { IconName } from '../components/ui/Icon'
import type { Role } from '../lib/labels'

export type NavItem = {
  path: string
  label: string
  shortLabel: string
  icon: IconName
  roles: Role[]
  description: string
}

const ALL_ROLES: Role[] = [
  'CHEF_CHANTIER',
  'RESPONSABLE_MATERIEL',
  'RESPONSABLE_ACHATS',
  'DAF',
  'DIRECTION_GENERALE',
  'ADMIN_SYSTEME',
]

export const NAV_ITEMS: NavItem[] = [
  {
    path: '/',
    label: 'Tableau de bord',
    shortLabel: 'Dashboard',
    icon: 'dashboard',
    roles: ALL_ROLES,
    description: 'KPIs, disponibilité du parc et exécution budgétaire.',
  },
  {
    path: '/besoins',
    label: 'Besoins matériel',
    shortLabel: 'Besoins',
    icon: 'clipboard',
    roles: ['CHEF_CHANTIER', 'RESPONSABLE_MATERIEL', 'DIRECTION_GENERALE', 'DAF', 'ADMIN_SYSTEME'],
    description: 'Expression, validation et consolidation des besoins des chantiers.',
  },
  {
    path: '/parc',
    label: 'Parc & chantiers',
    shortLabel: 'Parc',
    icon: 'truck',
    roles: ['CHEF_CHANTIER', 'RESPONSABLE_MATERIEL', 'DIRECTION_GENERALE', 'RESPONSABLE_ACHATS', 'DAF', 'ADMIN_SYSTEME'],
    description: 'Engins, carte GPS, affectations, pannes et ordres de travail.',
  },
  {
    path: '/achats',
    label: 'Achats & marchés',
    shortLabel: 'Achats',
    icon: 'shopping',
    roles: ['RESPONSABLE_ACHATS', 'DIRECTION_GENERALE', 'DAF', 'ADMIN_SYSTEME'],
    description: 'Appels d’offres, comparatifs multicritères, contrats et commandes.',
  },
  {
    path: '/fournisseurs',
    label: 'Fournisseurs',
    shortLabel: 'Fournisseurs',
    icon: 'users',
    roles: ['RESPONSABLE_ACHATS', 'RESPONSABLE_MATERIEL', 'DIRECTION_GENERALE', 'ADMIN_SYSTEME'],
    description: 'Annuaire, évaluation R07, catalogue, classement et blacklistage.',
  },
  {
    path: '/finance',
    label: 'Espace DAF',
    shortLabel: 'Finance',
    icon: 'wallet',
    roles: ['DAF', 'DIRECTION_GENERALE', 'ADMIN_SYSTEME'],
    description: 'Engagements budgétaires, factures et mandatements.',
  },
  {
    path: '/admin',
    label: 'Administration',
    shortLabel: 'Admin',
    icon: 'shield',
    roles: ['ADMIN_SYSTEME'],
    description: 'Comptes utilisateurs, habilitations et journal d’audit immuable.',
  },
  {
    path: '/chatbot',
    label: 'Assistant IA',
    shortLabel: 'Assistant',
    icon: 'bot',
    roles: ALL_ROLES,
    description: 'Assistant conversationnel connecté aux référentiels CSE.',
  },
]

export function navItemsForRole(role: Role): NavItem[] {
  return NAV_ITEMS.filter((item) => item.roles.includes(role))
}

export function canAccess(role: Role, pathname: string): boolean {
  const item = NAV_ITEMS.find((candidate) =>
    candidate.path === '/' ? pathname === '/' : pathname === candidate.path || pathname.startsWith(`${candidate.path}/`),
  )
  if (!item) return true
  return item.roles.includes(role)
}

export function navItemForPath(pathname: string): NavItem | undefined {
  return [...NAV_ITEMS]
    .sort((a, b) => b.path.length - a.path.length)
    .find((candidate) =>
      candidate.path === '/' ? pathname === '/' : pathname === candidate.path || pathname.startsWith(`${candidate.path}/`),
    )
}
