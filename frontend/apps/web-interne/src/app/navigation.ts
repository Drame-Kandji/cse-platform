import {
  Activity,
  BarChart3,
  Building2,
  CalendarRange,
  ClipboardCheck,
  ClipboardList,
  FileSignature,
  Gavel,
  History,
  LayoutDashboard,
  Library,
  Map as MapIcon,
  Truck,
  Users,
  Wallet,
  Wrench,
  Handshake,
  type LucideIcon,
} from 'lucide-react'
import type navigationFr from '@/locales/fr/navigation.json'
import type { Role } from '@/shared/domain/labels'
import { access } from './access'
import { paths } from './routes'

export type NavItemKey = keyof (typeof navigationFr)['items']
export type NavGroupKey = keyof (typeof navigationFr)['groups']

export interface NavItem {
  /** Cle i18n dans `navigation:items`. */
  readonly key: NavItemKey
  readonly path: string
  readonly icon: LucideIcon
  readonly roles: readonly Role[]
  /** Mots-cles supplementaires pour la palette de commandes. */
  readonly keywords?: readonly string[]
  /** Actif uniquement sur le chemin exact (tableau de bord). */
  readonly end?: boolean
}

export interface NavGroup {
  readonly key: NavGroupKey
  readonly items: readonly NavItem[]
}

/**
 * Navigation principale : SOURCE UNIQUE des menus (sidebar, tiroir mobile, palette).
 * Ajouter un ecran = une entree ici + une route dans router.tsx + des droits dans access.ts.
 */
export const NAVIGATION: readonly NavGroup[] = [
  {
    key: 'pilotage',
    items: [
      {
        key: 'dashboard',
        path: paths.dashboard,
        icon: LayoutDashboard,
        roles: access.dashboard,
        end: true,
        keywords: ['accueil', 'kpi'],
      },
      {
        key: 'rapports',
        path: paths.rapports,
        icon: BarChart3,
        roles: access.rapports,
        keywords: ['export', 'pdf', 'excel'],
      },
    ],
  },
  {
    key: 'besoins',
    items: [
      {
        key: 'besoins',
        path: paths.besoins,
        icon: ClipboardList,
        roles: access.besoins,
        keywords: ['demande', 'expression'],
      },
      {
        key: 'validation',
        path: paths.validation,
        icon: ClipboardCheck,
        roles: access.validation,
        keywords: ['valider', 'rejeter', 'arbitrage'],
      },
    ],
  },
  {
    key: 'parc',
    items: [
      {
        key: 'materiels',
        path: paths.materiels,
        icon: Truck,
        roles: access.materiels,
        keywords: ['engin', 'equipement', 'inventaire'],
      },
      {
        key: 'carte',
        path: paths.carte,
        icon: MapIcon,
        roles: access.carte,
        keywords: ['gps', 'localisation'],
      },
      {
        key: 'planning',
        path: paths.planning,
        icon: CalendarRange,
        roles: access.planning,
        keywords: ['affectation', 'gantt'],
      },
      {
        key: 'chantiers',
        path: paths.chantiers,
        icon: Building2,
        roles: access.chantiers,
        keywords: ['site', 'projet'],
      },
      {
        key: 'maintenances',
        path: paths.maintenances,
        icon: Wrench,
        roles: access.maintenances,
        keywords: ['panne', 'reparation', 'entretien'],
      },
    ],
  },
  {
    key: 'achats',
    items: [
      {
        key: 'appelsOffres',
        path: paths.appelsOffres,
        icon: Gavel,
        roles: access.appelsOffres,
        keywords: ['ao', 'consultation', 'offre'],
      },
      {
        key: 'contrats',
        path: paths.contrats,
        icon: FileSignature,
        roles: access.contrats,
        keywords: ['commande', 'livraison'],
      },
      {
        key: 'fournisseurs',
        path: paths.fournisseurs,
        icon: Handshake,
        roles: access.fournisseurs,
        keywords: ['prestataire', 'evaluation'],
      },
    ],
  },
  {
    key: 'finance',
    items: [
      {
        key: 'finance',
        path: paths.finance,
        icon: Wallet,
        roles: access.finance,
        keywords: ['budget', 'facture', 'engagement'],
      },
    ],
  },
  {
    key: 'admin',
    items: [
      {
        key: 'utilisateurs',
        path: paths.adminUtilisateurs,
        icon: Users,
        roles: access.admin,
        keywords: ['compte', 'role'],
      },
      {
        key: 'referentiels',
        path: paths.adminReferentiels,
        icon: Library,
        roles: access.admin,
        keywords: ['parametres'],
      },
      {
        key: 'audit',
        path: paths.adminAudit,
        icon: History,
        roles: access.admin,
        keywords: ['journal', 'trace'],
      },
      {
        key: 'plateforme',
        path: paths.adminPlateforme,
        icon: Activity,
        roles: access.admin,
        keywords: ['services', 'sante', 'supervision'],
      },
    ],
  },
]

/**
 * Filtre la navigation selon les roles de l'utilisateur (groupes vides retires).
 * @param hasAnyRole fonction issue de `useAuth()`
 */
export function navigationFor(hasAnyRole: (roles: readonly Role[]) => boolean): NavGroup[] {
  return NAVIGATION.map((group) => ({
    ...group,
    items: group.items.filter((item) => hasAnyRole(item.roles)),
  })).filter((group) => group.items.length > 0)
}
