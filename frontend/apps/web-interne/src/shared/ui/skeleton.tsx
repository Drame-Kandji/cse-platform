import type { HTMLAttributes } from 'react'
import { Loader2 } from 'lucide-react'
import { cn } from '@/shared/lib/cn'

/** Bloc de chargement anime (forme du contenu a venir). */
export function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      aria-hidden
      className={cn(
        'relative overflow-hidden rounded-md bg-muted before:absolute before:inset-0 before:-translate-x-full before:animate-shimmer before:bg-gradient-to-r before:from-transparent before:via-surface/60 before:to-transparent',
        className,
      )}
      {...props}
    />
  )
}

/** Indicateur de chargement circulaire avec libelle accessible. */
export function Spinner({ className, label }: { className?: string; label?: string }) {
  return (
    <span role="status" className={cn('inline-flex items-center gap-2 text-muted-foreground', className)}>
      <Loader2 className="size-4 animate-spin" aria-hidden />
      {label ? <span className="text-sm">{label}</span> : <span className="sr-only">Chargement</span>}
    </span>
  )
}

/** Barre de progression indeterminee (rechargement en arriere-plan). */
export function ProgressBar({ active, className }: { active: boolean; className?: string }) {
  return (
    <div className={cn('h-0.5 w-full overflow-hidden', className)} aria-hidden>
      {active && <div className="h-full w-2/5 animate-indeterminate rounded-full bg-primary" />}
    </div>
  )
}
