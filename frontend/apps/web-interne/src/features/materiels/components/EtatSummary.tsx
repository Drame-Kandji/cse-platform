import { describeStatus, type EtatMateriel } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatNumber, formatPercent } from '@/shared/lib/format'
import { Skeleton } from '@/shared/ui/skeleton'
import { dotToneClasses } from '@/shared/ui/tone'
import type { EtatCount } from '../hooks/useParcQueries'

export interface EtatSummaryProps {
  counts: readonly EtatCount[]
  total: number
  loading: boolean
  selected: EtatMateriel | null
  onToggle: (etat: EtatMateriel) => void
}

/** Bandeau de synthese : effectif par etat (filtres courants) ; un clic filtre la liste. */
export function EtatSummary({ counts, total, loading, selected, onToggle }: EtatSummaryProps) {
  return (
    <div
      className="mb-4 grid grid-cols-2 gap-2 sm:gap-3 lg:grid-cols-4"
      role="group"
      aria-label="Répartition par état"
    >
      {counts.map(({ etat, count }) => {
        const descriptor = describeStatus('etatMateriel', etat)
        const active = selected === etat
        return (
          <button
            key={etat}
            type="button"
            aria-pressed={active}
            onClick={() => {
              onToggle(etat)
            }}
            className={cn(
              'group flex flex-col gap-1.5 rounded-xl border bg-surface px-4 py-3 text-left shadow-card transition-colors',
              active
                ? 'border-primary/50 ring-1 ring-primary/30'
                : 'border-border hover:border-border-strong',
            )}
          >
            <span className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
              <span className={cn('size-2 rounded-full', dotToneClasses[descriptor.tone])} aria-hidden />
              {descriptor.label}
            </span>
            {loading || count === undefined ? (
              <Skeleton className="h-7 w-14" />
            ) : (
              <span className="flex items-baseline gap-2">
                <span className="text-2xl font-semibold tabular-nums tracking-tight">
                  {formatNumber(count)}
                </span>
                {total > 0 && (
                  <span className="text-xs tabular-nums text-muted-foreground">
                    {formatPercent(count / total, { decimals: 0 })}
                  </span>
                )}
              </span>
            )}
          </button>
        )
      })}
    </div>
  )
}
