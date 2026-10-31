import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Card } from '@/shared/ui/card'
import { latenceTone, type ServiceEtat } from '../lib/plateforme'

/** Carte de sante d'un service : nom, statut, latence (rouge seulement si indisponible ou tres lent). */
export function ServiceCard({ service }: { service: ServiceEtat }) {
  const tone = latenceTone(service)
  const up = service.statut === 'UP'
  return (
    <Card className={cn('flex flex-col gap-3 p-4', !up && 'border-danger/40')}>
      <div className="flex items-center justify-between gap-2">
        <span className="truncate font-mono text-sm font-medium">{service.nom}</span>
        <Badge tone={up ? 'neutral' : 'danger'} size="sm">
          {up ? 'Opérationnel' : 'Indisponible'}
        </Badge>
      </div>
      <div className="flex items-baseline justify-between text-sm">
        <span className="text-muted-foreground">Latence</span>
        <span
          className={cn(
            'font-semibold tabular-nums',
            tone === 'danger' ? 'text-danger-emphasis' : 'text-foreground',
          )}
        >
          {up && service.latenceMs != null ? `${formatNumber(service.latenceMs)} ms` : '—'}
        </span>
      </div>
    </Card>
  )
}
