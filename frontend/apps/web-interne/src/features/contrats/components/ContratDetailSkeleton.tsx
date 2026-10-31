import { Skeleton } from '@/shared/ui'

/** Squelette de chargement du detail d'un contrat. */
export function ContratDetailSkeleton() {
  return (
    <div className="grid gap-6" aria-busy>
      <Skeleton className="h-8 w-64" />
      <Skeleton className="h-20 w-full" />
      <div className="grid gap-6 lg:grid-cols-3">
        <Skeleton className="h-64 lg:col-span-2" />
        <Skeleton className="h-64" />
      </div>
    </div>
  )
}
