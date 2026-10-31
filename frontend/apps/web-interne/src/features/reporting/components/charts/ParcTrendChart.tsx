import { LineChart as LineIcon } from 'lucide-react'
import { Area, CartesianGrid, ComposedChart, Line, Tooltip, XAxis, YAxis } from 'recharts'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { ChartContainer, ChartLegend, ChartTooltipContent } from '@/shared/ui/chart'
import { axisProps, chartColor, cursorProps, gridProps } from '@/shared/ui/chart-theme'
import { EmptyState } from '@/shared/ui/empty-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { formatTaux } from '../../lib/format'
import { periodeLabel } from '../../lib/periode-label'
import type { Point } from '../../types'

export interface ParcTrendChartProps {
  disponibilite?: readonly Point[]
  utilisation?: readonly Point[]
  loading?: boolean
}

interface Row {
  periode: string
  disponibilite?: number
  utilisation?: number
}

/** Fusionne deux series par periode (ordre chronologique conserve). */
function merge(disponibilite: readonly Point[], utilisation: readonly Point[]): Row[] {
  const rows = new Map<string, Row>()
  for (const point of disponibilite)
    rows.set(point.periode, { periode: point.periode, disponibilite: point.valeur })
  for (const point of utilisation) {
    rows.set(point.periode, {
      ...(rows.get(point.periode) ?? { periode: point.periode }),
      utilisation: point.valeur,
    })
  }
  return [...rows.values()].sort((a, b) => a.periode.localeCompare(b.periode))
}

/** Evolution des taux de disponibilite et d'utilisation du parc (aire + courbe, 0-100 %). */
export function ParcTrendChart({ disponibilite = [], utilisation = [], loading }: ParcTrendChartProps) {
  const data = merge(disponibilite, utilisation)
  return (
    <Card className="flex h-full flex-col">
      <CardHeader
        title="Disponibilité et utilisation du parc"
        description="Taux sur la période, en %"
        action={
          <ChartLegend
            items={[
              { label: 'Disponibilité', color: chartColor(0) },
              { label: 'Utilisation', color: chartColor(1) },
            ]}
          />
        }
      />
      <CardContent className="flex-1">
        {!loading && data.length === 1 && (
          <p className="mb-2 text-xs text-muted-foreground">
            Historique en cours de constitution : un seul relevé disponible sur la période.
          </p>
        )}
        {loading ? (
          <Skeleton className="h-[280px] w-full" />
        ) : data.length === 0 ? (
          <EmptyState
            size="compact"
            icon={<LineIcon aria-hidden />}
            title="Pas encore assez de données"
            description="Les taux apparaîtront après quelques jours d'activité sur la période."
          />
        ) : (
          <ChartContainer
            height={280}
            label="Évolution des taux de disponibilité et d'utilisation du parc sur la période"
          >
            <ComposedChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="grad-disponibilite" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stopColor={chartColor(0)} stopOpacity={0.22} />
                  <stop offset="100%" stopColor={chartColor(0)} stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid {...gridProps} />
              <XAxis dataKey="periode" {...axisProps} tickFormatter={periodeLabel} minTickGap={24} />
              <YAxis
                {...axisProps}
                width={44}
                domain={[0, 100]}
                ticks={[0, 25, 50, 75, 100]}
                tickFormatter={(value: number) => formatTaux(value, 0)}
              />
              <Tooltip
                cursor={cursorProps}
                content={
                  <ChartTooltipContent
                    valueFormatter={(value) => formatTaux(value)}
                    labelFormatter={periodeLabel}
                  />
                }
              />
              <Area
                type="monotone"
                dataKey="disponibilite"
                name="Disponibilité"
                stroke={chartColor(0)}
                strokeWidth={2}
                fill="url(#grad-disponibilite)"
                connectNulls
                dot={data.length === 1 ? { r: 4, fill: chartColor(0) } : false}
                activeDot={{ r: 4, strokeWidth: 2, stroke: 'rgb(var(--surface))' }}
              />
              <Line
                type="monotone"
                dataKey="utilisation"
                name="Utilisation"
                stroke={chartColor(1)}
                strokeWidth={2}
                dot={data.length === 1 ? { r: 4, fill: chartColor(1) } : false}
                connectNulls
                activeDot={{ r: 4, strokeWidth: 2, stroke: 'rgb(var(--surface))' }}
              />
            </ComposedChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  )
}
