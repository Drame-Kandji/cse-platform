import type { ReactNode } from 'react'
import { Skeleton } from '@/shared/ui/skeleton'

/** Paire libelle / valeur d'une fiche (`multiline` : texte libre, conserve les retours a la ligne). */
export function Fact({ label, children, multiline }: { label: string; children: ReactNode; multiline?: boolean }) {
  return (
    <div className="grid content-start gap-0.5">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className={multiline ? 'whitespace-pre-line text-sm' : 'text-sm font-medium tabular-nums'}>
        {children}
      </dd>
    </div>
  )
}

/** Squelette de la fiche besoin. */
export function DetailSkeleton() {
  return (
    <div className="grid gap-6" aria-busy>
      <Skeleton className="h-9 w-80" />
      <Skeleton className="h-10 w-full max-w-3xl" />
      <div className="grid gap-6 lg:grid-cols-3">
        <Skeleton className="h-64 lg:col-span-2" />
        <Skeleton className="h-64" />
      </div>
    </div>
  )
}
