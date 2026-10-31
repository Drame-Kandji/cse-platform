import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

export interface PageHeaderProps {
  title: ReactNode
  /**
   * Ligne courte et factuelle sous le titre (chantier, NINEA...), une seule ligne discrete.
   * Pas de phrase d'accroche ni d'explication : charte sobre = titre + action principale.
   */
  subtitle?: ReactNode
  /** Boutons d'action (a droite sur desktop, sous le titre sur mobile). */
  actions?: ReactNode
  /** Fil d'Ariane personnalise (le fil global est deja dans la barre superieure). */
  breadcrumb?: ReactNode
  /** Badges / meta-donnees a cote du titre (statut, reference). */
  meta?: ReactNode
  className?: string
}

/** En-tete standard de toute page metier : titre (+ meta) a gauche, action(s) a droite. */
export function PageHeader({ title, subtitle, actions, breadcrumb, meta, className }: PageHeaderProps) {
  return (
    <header className={cn('mb-6 grid gap-4', className)}>
      {breadcrumb}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0 space-y-1">
          <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
            <h1 className="text-2xl font-semibold tracking-tight text-foreground">{title}</h1>
            {meta}
          </div>
          {subtitle && <p className="truncate text-sm text-muted-foreground">{subtitle}</p>}
        </div>
        {actions && <div className="flex flex-wrap items-center gap-2 sm:shrink-0">{actions}</div>}
      </div>
    </header>
  )
}
