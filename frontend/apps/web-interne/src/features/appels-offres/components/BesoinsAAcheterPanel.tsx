import { access } from '@/app/access'
import { Can } from '@/shared/auth'
import { labelOf } from '@/shared/domain/labels'
import { formatNumber, formatRelative } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { BesoinAAcheter } from '../api/types'
import { useBesoinsALancer } from '../hooks/useAppelsOffres'

/** Liste compacte des besoins a lancer, avec lancement d'un AO pre-rempli. */
export function BesoinsAAcheterPanel({ onLancer }: { onLancer: (besoin: BesoinAAcheter) => void }) {
  const { query, besoins } = useBesoinsALancer()
  if (query.isPending) {
    return (
      <Card className="grid gap-2 p-4">
        {[0, 1, 2].map((index) => (
          <Skeleton key={index} className="h-12 w-full" />
        ))}
      </Card>
    )
  }
  if (query.isError) {
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} size="compact" />
  }
  if (besoins.length === 0) {
    return (
      <Card>
        <EmptyState size="compact" title="Aucun besoin à lancer" />
      </Card>
    )
  }
  return (
    <Card>
      <ul aria-label="Besoins à acheter" className="divide-y divide-border">
        {besoins.map((besoin) => (
          <li key={besoin.besoinId} className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:px-5">
            <div className="min-w-0 flex-1">
              <p className="flex flex-wrap items-center gap-2 text-sm font-medium">
                <span className="truncate">{besoin.designation}</span>
                {besoin.priorite !== 'NORMAL' && (
                  <StatusBadge kind="priorite" value={besoin.priorite} size="sm" />
                )}
              </p>
              <p className="truncate text-xs text-muted-foreground">
                {besoin.reference} · {formatNumber(besoin.quantite)} ×{' '}
                {labelOf('categorieMateriel', besoin.categorie)} · {besoin.chantierNom} ·{' '}
                {formatRelative(besoin.dateReception)}
              </p>
            </div>
            <Can roles={access.achatsGestion}>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => {
                  onLancer(besoin)
                }}
                aria-label={`Lancer un appel d'offres pour ${besoin.reference}`}
              >
                Lancer un AO
              </Button>
            </Can>
          </li>
        ))}
      </ul>
    </Card>
  )
}
