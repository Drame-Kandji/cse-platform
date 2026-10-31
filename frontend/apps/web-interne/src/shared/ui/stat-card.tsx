import { ArrowDownRight, ArrowUpRight, Minus } from 'lucide-react'
import type { ReactNode } from 'react'
import type { Tone } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatPercent } from '@/shared/lib/format'
import { Card } from './card'
import { Skeleton } from './skeleton'
import { softToneClasses } from './tone'

export interface StatDelta {
  /** Variation relative (0.12 = +12 %). */
  readonly value: number
  /** Periode de comparaison, ex. "vs mois dernier". */
  readonly label?: string
  /** Faux si une hausse est une mauvaise nouvelle (pannes, couts). */
  readonly positiveIsGood?: boolean
}

export interface StatCardProps {
  label: ReactNode
  /** Valeur deja formatee (formatMoney, formatNumber...). */
  value: ReactNode
  icon?: ReactNode
  iconTone?: Tone
  delta?: StatDelta
  hint?: ReactNode
  /** Emplacement graphique (ex. <Sparkline values={...} />). */
  sparkline?: ReactNode
  loading?: boolean
  className?: string
}

function DeltaPill({ delta }: { delta: StatDelta }) {
  const direction = delta.value > 0 ? 'up' : delta.value < 0 ? 'down' : 'flat'
  const good = direction === 'flat' ? null : (direction === 'up') === (delta.positiveIsGood ?? true)
  const Icon = direction === 'up' ? ArrowUpRight : direction === 'down' ? ArrowDownRight : Minus
  return (
    <span className="inline-flex items-center gap-1.5 text-xs">
      <span
        className={cn(
          'inline-flex items-center gap-0.5 rounded-md px-1.5 py-0.5 font-medium tabular-nums',
          good === null ? softToneClasses.neutral : good ? softToneClasses.success : softToneClasses.danger,
        )}
      >
        <Icon className="size-3.5" aria-hidden />
        {formatPercent(delta.value, { signed: true })}
      </span>
      {delta.label && <span className="text-muted-foreground">{delta.label}</span>}
    </span>
  )
}

/** Indicateur cle (KPI) : libelle, valeur, variation et tendance. */
export function StatCard({
  label,
  value,
  icon,
  iconTone = 'primary',
  delta,
  hint,
  sparkline,
  loading,
  className,
}: StatCardProps) {
  return (
    <Card className={cn('flex flex-col gap-3 p-5', className)}>
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-medium text-muted-foreground">{label}</p>
        {icon && (
          <span
            className={cn(
              'grid size-8 shrink-0 place-items-center rounded-lg [&_svg]:size-4',
              softToneClasses[iconTone],
            )}
          >
            {icon}
          </span>
        )}
      </div>
      {loading ? (
        <div className="grid gap-2">
          <Skeleton className="h-8 w-32" />
          <Skeleton className="h-4 w-24" />
        </div>
      ) : (
        <>
          <p className="text-2xl font-semibold tabular-nums tracking-tight text-foreground">{value}</p>
          {(delta ?? hint) && (
            <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
              {delta && <DeltaPill delta={delta} />}
              {hint && <span className="text-xs text-muted-foreground">{hint}</span>}
            </div>
          )}
          {sparkline && <div className="-mx-1 mt-auto pt-1">{sparkline}</div>}
        </>
      )}
    </Card>
  )
}

/** Alias metier : KpiCard = StatCard. */
export const KpiCard = StatCard
