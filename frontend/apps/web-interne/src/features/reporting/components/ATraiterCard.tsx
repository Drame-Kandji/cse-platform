import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { Skeleton } from '@/shared/ui/skeleton'
import { useQueueCounts } from '../hooks/useReporting'
import type { QueueDef } from '../lib/queues'

/** Raccourcis vers les files de travail du role, avec leur compteur (bleu si non vide). */
export function ATraiterCard({ queues }: { queues: readonly QueueDef[] }) {
  const counts = useQueueCounts(queues)
  const total = counts.reduce((sum, result) => sum + (result.data ?? 0), 0)
  const loading = counts.some((result) => result.isPending)
  return (
    <Card className="flex h-full flex-col">
      <CardHeader title="À traiter" />
      <CardContent className="flex-1 px-3 pb-3 pt-3">
        <ul className="grid">
          {queues.map((queue, index) => {
            const result = counts[index]
            const count = result?.data
            const active = (count ?? 0) > 0
            return (
              <li key={queue.id}>
                <Link
                  to={queue.href}
                  title={queue.hint}
                  className="group flex min-h-11 items-center gap-3 rounded-lg px-2 py-2 transition-colors hover:bg-muted"
                >
                  <span className="min-w-0 flex-1 truncate text-sm">{queue.label}</span>
                  {result?.isPending ? (
                    <Skeleton className="h-5 w-7" />
                  ) : result?.isError ? (
                    <span className="text-xs text-muted-foreground" title="Compteur indisponible">
                      —
                    </span>
                  ) : (
                    <span
                      className={cn(
                        'min-w-7 rounded-full px-2 text-center text-xs font-semibold leading-5 tabular-nums',
                        active ? 'bg-primary/[0.08] text-primary-emphasis' : 'text-muted-foreground',
                      )}
                    >
                      {formatNumber(count)}
                    </span>
                  )}
                  <ChevronRight className="size-4 text-muted-foreground" aria-hidden />
                </Link>
              </li>
            )
          })}
        </ul>
        {!loading && total === 0 && (
          <p className="px-2 pt-2 text-xs text-muted-foreground">Vos files de travail sont à jour.</p>
        )}
      </CardContent>
    </Card>
  )
}
