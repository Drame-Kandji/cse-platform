import { Building2, FileSignature, Gavel, Home, Receipt, type LucideIcon } from 'lucide-react'
import { paths } from './routes'

export interface NavItem {
  readonly key: 'accueil' | 'appelsOffres' | 'contrats' | 'factures' | 'entreprise'
  readonly to: string
  readonly icon: LucideIcon
  /** Actif uniquement sur le chemin exact (Accueil). */
  readonly end?: boolean
}

/** Menu du portail (identique pour tout fournisseur). Libelles : `portail:nav.<key>`. */
export const NAV_ITEMS: readonly NavItem[] = [
  { key: 'accueil', to: paths.accueil, icon: Home, end: true },
  { key: 'appelsOffres', to: paths.appelsOffres, icon: Gavel },
  { key: 'contrats', to: paths.contrats, icon: FileSignature },
  { key: 'factures', to: paths.factures, icon: Receipt },
  { key: 'entreprise', to: paths.entreprise, icon: Building2 },
]
