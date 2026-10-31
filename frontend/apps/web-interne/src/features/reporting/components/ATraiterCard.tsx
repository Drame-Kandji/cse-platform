import { CircleCheck, ChevronRight, ListTodo } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { Skeleton } from '@/shared/ui/skeleton'
import { softToneClasses } from '@/shared/ui/tone'
import { useQueueCounts } from '../hooks/useReporting'
import type { QueueDef } from '../lib/queues'

/** Raccourcis vers les files de travail du role, avec leur compteur. */
export function ATraiterCard({ queues }: { queues: readonly QueueDef[] }) {
  const counts = useQueueCounts(queues)
  const total = counts.reduce((sum, result) => sum + (result.data ?? 0), 0)
  const loading = counts.some((result) => result.isPending)
  return (
    <Card className="flex h-full flex-col">
      <CardHeader
        title="À traiter"
        description={
          loading
            ? 'Chargement…'
            : total > 0
              ? `${formatNumber(total)} élément(s) en attente de votre action`
              : 'Rien en attente'
        }
        icon={<ListTodo />}
      />
      <CardContent className="flex-1 pt-4">
        <ul className="grid gap-2">
          {queues.map((queue, index) => {
            const result = counts[index]
            const count = result?.data
            const active = (count ?? 0) > 0
            return (
              <li key={queue.id}>
                <Link
                  to={queue.href}
                  className="group flex items-center gap-3 rounded-lg border border-border px-3 py-2.5 transition-colors hover:border-border-strong hover:bg-muted/60"
                >
                  <span className="grid min-w-0 flex-1">
                    <span className="truncate text-sm font-medium">{queue.label}</span>
                    <span className="truncate text-xs text-muted-foreground">{queue.hint}</span>
                  </span>
                  {result?.isPending ? (
                    <Skeleton className="h-6 w-8" />
                  ) : result?.isError ? (
                    <span className="text-xs text-muted-foreground" title="Compteur indisponible">
                      —
                    </span>
                  ) : (
                    <span
                      className={cn(
                        'min-w-8 rounded-md px-2 py-0.5 text-center text-sm font-semibold tabular-nums',
                        active ? softToneClasses[queue.tone] : 'text-muted-foreground',
                      )}
                    >
                      {formatNumber(count)}
                    </span>
                  )}
                  <ChevronRight
                    className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5"
                    aria-hidden
                  />
                </Link>
              </li>
            )
          })}
        </ul>
        {!loading && total === 0 && (
          <p className="mt-3 flex items-center gap-2 text-xs text-success-emphasis">
            <CircleCheck className="size-3.5" aria-hidden /> Vos files de travail sont à jour.
          </p>
        )}
      </CardContent>
    </Card>
  )
}
