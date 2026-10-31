import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

export interface PageHeaderProps {
  title: ReactNode
  description?: ReactNode
  /** Boutons d'action (a droite sur desktop, sous le titre sur mobile). */
  actions?: ReactNode
  /** Fil d'Ariane personnalise (le fil global est deja dans la barre superieure). */
  breadcrumb?: ReactNode
  /** Badges / meta-donnees a cote du titre (statut, reference). */
  meta?: ReactNode
  className?: string
}

/** En-tete standard de toute page metier. */
export function PageHeader({ title, description, actions, breadcrumb, meta, className }: PageHeaderProps) {
  return (
    <header className={cn('mb-6 grid gap-4 sm:mb-8', className)}>
      {breadcrumb}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="min-w-0 space-y-1.5">
          <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
            <h1 className="text-xl font-semibold tracking-tight text-foreground sm:text-2xl">{title}</h1>
            {meta}
          </div>
          {description && <p className="max-w-3xl text-sm text-muted-foreground">{description}</p>}
        </div>
        {actions && <div className="flex flex-wrap items-center gap-2 sm:shrink-0">{actions}</div>}
      </div>
    </header>
  )
}
