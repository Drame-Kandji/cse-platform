import { Server, ServerCrash } from 'lucide-react'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Card } from '@/shared/ui/card'
import { LATENCE_LENTE_MS, latenceTone, type ServiceEtat } from '../lib/plateforme'

/** Carte de sante d'un service : statut, latence et jauge. */
export function ServiceCard({ service }: { service: ServiceEtat }) {
  const tone = latenceTone(service)
  const up = service.statut === 'UP'
  return (
    <Card className={cn('flex flex-col gap-3 p-4', !up && 'border-danger/40')}>
      <div className="flex items-start justify-between gap-2">
        <span className="flex min-w-0 items-center gap-2.5">
          <span
            className={cn(
              'grid size-9 shrink-0 place-items-center rounded-lg',
              up ? 'bg-success/10 text-success-emphasis' : 'bg-danger/10 text-danger-emphasis',
            )}
          >
            {up ? <Server className="size-4" aria-hidden /> : <ServerCrash className="size-4" aria-hidden />}
          </span>
          <span className="truncate font-mono text-sm font-medium">{service.nom}</span>
        </span>
        <Badge tone={up ? 'success' : 'danger'} dot size="sm">
          {up ? 'Opérationnel' : 'Indisponible'}
        </Badge>
      </div>
      <div className="flex items-baseline justify-between text-sm">
        <span className="text-muted-foreground">Latence</span>
        <span
          className={cn(
            'font-semibold tabular-nums',
            tone === 'warning' && 'text-warning-emphasis',
            tone === 'danger' && 'text-danger-emphasis',
          )}
        >
          {up && service.latenceMs != null ? `${formatNumber(service.latenceMs)} ms` : '—'}
        </span>
      </div>
      <div className="h-1.5 overflow-hidden rounded-full bg-muted" aria-hidden>
        <div
          className={cn(
            'h-full rounded-full',
            tone === 'success' ? 'bg-success' : tone === 'warning' ? 'bg-warning' : 'bg-danger',
          )}
          style={{
            width: up
              ? `${Math.min(100, ((service.latenceMs ?? 0) / (LATENCE_LENTE_MS * 2)) * 100)}%`
              : '100%',
          }}
        />
      </div>
    </Card>
  )
}
