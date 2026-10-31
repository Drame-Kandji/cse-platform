import { Inbox } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

export interface EmptyStateProps {
  title: ReactNode
  description?: ReactNode
  icon?: ReactNode
  /** Action principale (ex. "Creer un besoin"). */
  action?: ReactNode
  className?: string
  /** "compact" pour l'interieur d'une carte ou d'un popover. */
  size?: 'default' | 'compact'
}

/** Etat vide explicite : dit pourquoi c'est vide et quoi faire ensuite. */
export function EmptyState({
  title,
  description,
  icon,
  action,
  className,
  size = 'default',
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center text-center',
        size === 'default' ? 'gap-3 px-6 py-14' : 'gap-2 px-4 py-8',
        className,
      )}
    >
      <div
        className={cn(
          'grid place-items-center rounded-xl border border-border bg-surface-sunken text-muted-foreground shadow-xs',
          size === 'default' ? 'size-12 [&_svg]:size-6' : 'size-10 [&_svg]:size-5',
        )}
      >
        {icon ?? <Inbox aria-hidden />}
      </div>
      <div className="grid max-w-sm gap-1">
        <p className="text-sm font-semibold text-foreground">{title}</p>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      {action && <div className="mt-1">{action}</div>}
    </div>
  )
}
