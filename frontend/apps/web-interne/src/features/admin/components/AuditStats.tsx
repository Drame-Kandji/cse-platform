import { CalendarClock, History, Layers } from 'lucide-react'
import { formatNumber } from '@/shared/lib/format'
import { Card } from '@/shared/ui/card'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatCard } from '@/shared/ui/stat-card'
import { useAuditStatistiques } from '../hooks/useAudit'

/** En-tete statistique du journal d'audit : volumes et repartition par service. */
export function AuditStats() {
  const stats = useAuditStatistiques()
  const parService = stats.data?.parService ?? []
  const max = Math.max(1, ...parService.map((item) => item.total))
  return (
    <div className="grid gap-4 lg:grid-cols-4">
      <StatCard
        label="Événements journalisés"
        value={formatNumber(stats.data?.total)}
        icon={<History />}
        loading={stats.isPending}
      />
      <StatCard
        label="Aujourd'hui"
        value={formatNumber(stats.data?.aujourdhui)}
        icon={<CalendarClock />}
        iconTone="info"
        loading={stats.isPending}
      />
      <StatCard
        label="7 derniers jours"
        value={formatNumber(stats.data?.septDerniersJours)}
        icon={<Layers />}
        iconTone="accent"
        loading={stats.isPending}
      />
      <Card className="p-5">
        <p className="mb-3 text-sm font-medium text-muted-foreground">Par service</p>
        {stats.isPending ? (
          <Skeleton className="h-16 w-full" />
        ) : (
          <ul className="grid gap-1.5">
            {parService.slice(0, 5).map((item) => (
              <li
                key={item.service}
                className="grid grid-cols-[6.5rem_minmax(0,1fr)_3rem] items-center gap-2 text-xs"
              >
                <span className="truncate">{item.service}</span>
                <span className="h-1.5 overflow-hidden rounded-full bg-muted" aria-hidden>
                  <span
                    className="block h-full rounded-full bg-chart-1"
                    style={{ width: `${(item.total / max) * 100}%` }}
                  />
                </span>
                <span className="text-right tabular-nums text-muted-foreground">
                  {formatNumber(item.total, { compact: true })}
                </span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
