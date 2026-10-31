import { LineChart as LineIcon } from 'lucide-react'
import { useState } from 'react'
import { Area, AreaChart, Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from 'recharts'
import { formatMoney, formatNumber } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { ChartContainer, ChartTooltipContent } from '@/shared/ui/chart'
import {
  axisProps,
  chartColor,
  chartMargin,
  cursorProps,
  gridProps,
  yAxisWidthFor,
} from '@/shared/ui/chart-theme'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Select } from '@/shared/ui/select'
import { Skeleton } from '@/shared/ui/skeleton'
import { useSerie } from '../hooks/useReporting'
import { compactMoney, compactNumber, formatTaux } from '../lib/format'
import { INDICATEUR_LABELS } from '../lib/kpi-tables'
import { periodeLabel } from '../lib/periode-label'
import { GRANULARITES, type Granularite, type Indicateur } from '../types'

const GRANULARITE_LABELS: Readonly<Record<Granularite, string>> = {
  JOUR: 'Jour',
  SEMAINE: 'Semaine',
  MOIS: 'Mois',
}

function formatterFor(indicateur: Indicateur): { value: (v: number) => string; axis: (v: number) => string } {
  switch (INDICATEUR_LABELS[indicateur].unite) {
    case 'pourcentage':
      return { value: (v) => formatTaux(v), axis: (v) => formatTaux(v, 0) }
    case 'montant':
      return { value: (v) => formatMoney(v), axis: compactMoney }
    case 'jours':
      return { value: (v) => `${formatNumber(v, { decimals: 1 })} j`, axis: (v) => `${formatNumber(v)} j` }
    default:
      return { value: (v) => formatNumber(v), axis: compactNumber }
  }
}

export interface SeriesExplorerProps {
  /** Indicateurs accessibles pour la vue courante. */
  indicateurs: readonly Indicateur[]
  du: string
  au: string
  chantierId?: string
  defaultGranularite: Granularite
}

/** Explorateur de series : choix de l'indicateur et de la granularite, courbe et statistiques. */
export function SeriesExplorer({ indicateurs, du, au, chantierId, defaultGranularite }: SeriesExplorerProps) {
  const [choix, setChoix] = useState<Indicateur | undefined>()
  const [granularite, setGranularite] = useState<Granularite>(defaultGranularite)
  const indicateur = choix && indicateurs.includes(choix) ? choix : indicateurs[0]
  const query = useSerie(
    { indicateur: indicateur ?? 'TAUX_DISPONIBILITE', granularite, du, au, chantierId },
    { enabled: indicateur !== undefined },
  )
  if (!indicateur) return null
  const meta = INDICATEUR_LABELS[indicateur]
  const format = formatterFor(indicateur)
  const points = query.data ?? []
  const values = points.map((point) => point.valeur)
  const axisWidth = yAxisWidthFor(meta.unite === 'pourcentage' ? [...values, 100] : values, format.axis)
  const cumul = meta.unite === 'montant' || meta.unite === 'nombre'
  const stats =
    values.length > 0
      ? [
          {
            label: cumul ? 'Total' : 'Moyenne',
            value: cumul
              ? values.reduce((a, b) => a + b, 0)
              : values.reduce((a, b) => a + b, 0) / values.length,
          },
          { label: 'Minimum', value: Math.min(...values) },
          { label: 'Maximum', value: Math.max(...values) },
          { label: 'Dernière valeur', value: values[values.length - 1] ?? 0 },
        ]
      : []

  return (
    <Card>
      <CardHeader
        title="Explorateur de séries"
        description="Évolution d'un indicateur sur la période sélectionnée"
        action={
          <div className="flex flex-wrap items-center gap-2">
            <Select
              aria-label="Indicateur"
              className="w-auto min-w-56"
              value={indicateur}
              onValueChange={setChoix}
              options={indicateurs.map((value) => ({ value, label: INDICATEUR_LABELS[value].label }))}
            />
            <div
              role="radiogroup"
              aria-label="Granularité"
              className="inline-flex rounded-lg border border-border bg-surface p-0.5"
            >
              {GRANULARITES.map((value) => (
                <button
                  key={value}
                  type="button"
                  role="radio"
                  aria-checked={granularite === value}
                  onClick={() => {
                    setGranularite(value)
                  }}
                  className={
                    granularite === value
                      ? 'h-8 rounded-md bg-muted px-3 text-sm font-medium text-foreground'
                      : 'h-8 rounded-md px-3 text-sm text-muted-foreground hover:text-foreground'
                  }
                >
                  {GRANULARITE_LABELS[value]}
                </button>
              ))}
            </div>
          </div>
        }
        className="flex-wrap"
      />
      <CardContent className="grid gap-5">
        {stats.length > 0 && (
          <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {stats.map((stat) => (
              <div key={stat.label} className="rounded-lg bg-surface-sunken px-3 py-2">
                <dt className="text-xs text-muted-foreground">{stat.label}</dt>
                <dd className="text-sm font-semibold tabular-nums">{format.value(stat.value)}</dd>
              </div>
            ))}
          </dl>
        )}
        {query.isPending ? (
          <Skeleton className="h-72 w-full" />
        ) : query.isError ? (
          <ErrorState size="compact" error={query.error} onRetry={() => void query.refetch()} />
        ) : points.length === 0 ? (
          <EmptyState
            size="compact"
            icon={<LineIcon aria-hidden />}
            title="Aucune donnée"
            description="Aucune valeur pour cet indicateur sur la période."
          />
        ) : (
          <ChartContainer
            height={288}
            label={`${meta.label} par ${GRANULARITE_LABELS[granularite].toLowerCase()}`}
          >
            {cumul ? (
              <BarChart data={points} margin={chartMargin}>
                <CartesianGrid {...gridProps} />
                <XAxis dataKey="periode" {...axisProps} tickFormatter={periodeLabel} minTickGap={16} />
                <YAxis {...axisProps} width={axisWidth} tickFormatter={format.axis} />
                <Tooltip
                  cursor={cursorProps}
                  content={
                    <ChartTooltipContent valueFormatter={format.value} labelFormatter={periodeLabel} />
                  }
                />
                <Bar
                  dataKey="valeur"
                  name={meta.label}
                  fill={chartColor(0)}
                  radius={[4, 4, 0, 0]}
                  maxBarSize={32}
                />
              </BarChart>
            ) : (
              <AreaChart data={points} margin={chartMargin}>
                <defs>
                  <linearGradient id="grad-serie" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0%" stopColor={chartColor(0)} stopOpacity={0.2} />
                    <stop offset="100%" stopColor={chartColor(0)} stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid {...gridProps} />
                <XAxis dataKey="periode" {...axisProps} tickFormatter={periodeLabel} minTickGap={16} />
                <YAxis
                  {...axisProps}
                  width={axisWidth}
                  tickFormatter={format.axis}
                  domain={meta.unite === 'pourcentage' ? [0, 100] : ['auto', 'auto']}
                />
                <Tooltip
                  cursor={cursorProps}
                  content={
                    <ChartTooltipContent valueFormatter={format.value} labelFormatter={periodeLabel} />
                  }
                />
                <Area
                  type="monotone"
                  dataKey="valeur"
                  name={meta.label}
                  stroke={chartColor(0)}
                  strokeWidth={2}
                  fill="url(#grad-serie)"
                  dot={points.length === 1 ? { r: 4, fill: chartColor(0) } : false}
                  activeDot={{ r: 4, strokeWidth: 2, stroke: 'rgb(var(--surface))' }}
                />
              </AreaChart>
            )}
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  )
}
