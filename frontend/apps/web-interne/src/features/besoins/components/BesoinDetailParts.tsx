import type { ReactNode } from 'react'
import { Skeleton } from '@/shared/ui/skeleton'

/** Paire libelle / valeur d'une fiche. */
export function Fact({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="grid gap-0.5">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-sm font-medium tabular-nums">{children}</dd>
    </div>
  )
}

/** Squelette de la fiche besoin. */
export function DetailSkeleton() {
  return (
    <div className="grid gap-6" aria-busy>
      <Skeleton className="h-9 w-80" />
      <Skeleton className="h-24 w-full" />
      <div className="grid gap-6 lg:grid-cols-3">
        <Skeleton className="h-64 lg:col-span-2" />
        <Skeleton className="h-64" />
      </div>
    </div>
  )
}
