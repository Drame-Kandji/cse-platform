import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

export interface StatusPageProps {
  /** Code affiche en grand (403, 404...). */
  code?: string
  icon: ReactNode
  title: ReactNode
  description: ReactNode
  actions?: ReactNode
  children?: ReactNode
  className?: string
}

/** Page d'etat plein ecran (403, 404, erreur de chargement). */
export function StatusPage({
  code,
  icon,
  title,
  description,
  actions,
  children,
  className,
}: StatusPageProps) {
  return (
    <div
      className={cn(
        'flex min-h-[60vh] flex-col items-center justify-center px-4 py-16 text-center',
        className,
      )}
    >
      <div className="relative mb-6">
        <div className="grid size-16 place-items-center rounded-2xl border border-border bg-surface text-muted-foreground shadow-card [&_svg]:size-7">
          {icon}
        </div>
      </div>
      {code && (
        <p className="font-mono text-sm font-semibold tracking-widest text-primary-emphasis">{code}</p>
      )}
      <h1 className="mt-2 text-2xl font-semibold tracking-tight sm:text-3xl">{title}</h1>
      <p className="mt-3 max-w-md text-sm text-muted-foreground sm:text-base">{description}</p>
      {children}
      {actions && <div className="mt-8 flex flex-wrap items-center justify-center gap-3">{actions}</div>}
    </div>
  )
}
