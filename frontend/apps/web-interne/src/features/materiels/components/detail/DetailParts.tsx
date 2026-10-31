import { MapPin } from 'lucide-react'
import type { MaterielDetail } from '@/shared/domain/types'
import { Skeleton } from '@/shared/ui/skeleton'

/** Squelette de la fiche engin. */
export function DetailSkeleton() {
  return (
    <div className="grid gap-6" aria-busy>
      <div className="grid gap-2">
        <Skeleton className="h-8 w-72" />
        <Skeleton className="h-4 w-96 max-w-full" />
      </div>
      <Skeleton className="h-36 rounded-xl" />
      <Skeleton className="h-64 rounded-xl" />
    </div>
  )
}

/** Sous-titre : localisation actuelle (les autres faits sont dans la grille). */
export function MaterielSubtitle({ materiel }: { materiel: MaterielDetail }) {
  return (
    <span className="inline-flex items-center gap-1">
      <MapPin className="size-3.5" aria-hidden />
      {materiel.chantierActuelNom ?? 'Au dépôt'}
    </span>
  )
}
