import type { ReactNode } from 'react'
import type { Tone } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatPercent } from '@/shared/lib/format'
import { Card } from './card'
import { Skeleton } from './skeleton'

export interface StatDelta {
  /** Variation relative (0.12 = +12 %). */
  readonly value: number
  /** Periode de comparaison, ex. "vs mois dernier". */
  readonly label?: string
  /** Conserve pour compatibilite : la variation est affichee en texte neutre. */
  readonly positiveIsGood?: boolean
}

export interface StatCardProps {
  label: ReactNode
  /** Valeur deja formatee (formatMoney, formatNumber...). */
  value: ReactNode
  /** Conserve pour compatibilite : plus affiche (pas de tuile d'icone). */
  icon?: ReactNode
  /** Conserve pour compatibilite : plus utilise. */
  iconTone?: Tone
  /** Variation, affichee en texte discret dans la ligne secondaire. */
  delta?: StatDelta
  /** Une seule ligne courte et discrete sous la valeur. */
  hint?: ReactNode
  /** Mini-courbe : affichee seulement avec `showSparkline`. */
  sparkline?: ReactNode
  /** Opt-in : afficher la sparkline fournie. */
  showSparkline?: boolean
  /** Met la valeur en rouge (alerte reelle : retard, panne bloquante...). */
  alert?: boolean
  loading?: boolean
  className?: string
}

/** Indicateur cle (KPI) sobre : libelle, grand chiffre, au plus une ligne secondaire. */
export function StatCard({
  label,
  value,
  delta,
  hint,
  sparkline,
  showSparkline = false,
  alert = false,
  loading,
  className,
}: StatCardProps) {
  const secondary =
    delta || hint ? (
      <>
        {delta && (
          <span className="tabular-nums">
            {formatPercent(delta.value, { signed: true })}
            {delta.label ? ` ${delta.label}` : ''}
          </span>
        )}
        {delta && hint ? ' · ' : null}
        {hint}
      </>
    ) : null
  return (
    <Card className={cn('flex min-w-0 flex-col gap-2 p-4 sm:p-5', className)}>
      <p className="truncate text-sm text-muted-foreground">{label}</p>
      {loading ? (
        <div className="grid gap-2">
          <Skeleton className="h-8 w-28" />
          <Skeleton className="h-3.5 w-24" />
        </div>
      ) : (
        <>
          <p
            className={cn(
              'truncate text-xl font-semibold tabular-nums tracking-tight sm:text-2xl',
              alert ? 'text-danger-emphasis' : 'text-foreground',
            )}
          >
            {value}
          </p>
          {secondary && <p className="truncate text-xs text-muted-foreground">{secondary}</p>}
          {showSparkline && sparkline && <div className="-mx-1 mt-auto pt-1">{sparkline}</div>}
        </>
      )}
    </Card>
  )
}

/** Alias metier : KpiCard = StatCard. */
export const KpiCard = StatCard
