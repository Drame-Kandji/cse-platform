import { MapPin } from 'lucide-react'
import { labelOf } from '@/shared/domain/labels'
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
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-5">
        {Array.from({ length: 5 }, (_, index) => (
          <Skeleton key={index} className="h-32 rounded-xl" />
        ))}
      </div>
      <Skeleton className="h-64 rounded-xl" />
    </div>
  )
}

/** Sous-titre : marque, modele, type et localisation. */
export function MaterielSubtitle({ materiel }: { materiel: MaterielDetail }) {
  const modele = [materiel.marque, materiel.modele].filter(Boolean).join(' ')
  return (
    <span className="flex flex-wrap items-center gap-x-3 gap-y-1">
      <span>
        {[modele, materiel.typeEnginLibelle ?? labelOf('categorieMateriel', materiel.categorie)]
          .filter(Boolean)
          .join(' · ')}
      </span>
      <span className="inline-flex items-center gap-1">
        <MapPin className="size-3.5" aria-hidden />
        {materiel.chantierActuelNom ?? 'Au dépôt (aucun chantier)'}
      </span>
    </span>
  )
}
