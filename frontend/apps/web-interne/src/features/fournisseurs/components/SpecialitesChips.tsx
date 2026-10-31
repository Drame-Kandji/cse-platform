import { labelOf, type CategorieMateriel } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { Badge } from '@/shared/ui/badge'

/** Puces des specialites d'un fournisseur. */
export function SpecialitesChips({
  specialites,
  className,
}: {
  specialites: readonly CategorieMateriel[]
  className?: string
}) {
  if (specialites.length === 0) return <span className="text-sm text-muted-foreground">—</span>
  return (
    <ul className={cn('flex flex-wrap gap-1', className)} aria-label="Spécialités">
      {specialites.map((specialite) => (
        <li key={specialite}>
          <Badge tone="neutral" variant="outline" size="sm">
            {labelOf('categorieMateriel', specialite)}
          </Badge>
        </li>
      ))}
    </ul>
  )
}
