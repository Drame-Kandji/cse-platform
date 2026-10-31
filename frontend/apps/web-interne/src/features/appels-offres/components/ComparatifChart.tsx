import { Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from 'recharts'
import { describeStatus, type StatutAO } from '@/shared/domain/labels'
import { formatNumber } from '@/shared/lib/format'
import { ChartContainer, ChartLegend, ChartTooltipContent } from '@/shared/ui/chart'
import {
  axisProps,
  chartColor,
  chartMargin,
  cursorProps,
  gridProps,
  yAxisWidth,
} from '@/shared/ui/chart-theme'
import type { Comparatif } from '../api/types'
import { nomOffre } from './offre-display'

/** Nombre maximal d'offres tracees (palette de 6 couleurs, jamais cyclee). */
export const MAX_OFFRES_GRAPHIQUE = 6

/** Notes par critere de chaque offre (barres groupees ; couleur fixee par offre). */
export function ComparatifChart({ comparatif, statut }: { comparatif: Comparatif; statut: StatutAO }) {
  const offres = comparatif.offres.slice(0, MAX_OFFRES_GRAPHIQUE)
  const series = offres.map((offre, index) => ({
    key: `o${index}`,
    label: nomOffre(offre, statut),
    color: chartColor(index),
    offre,
  }))
  const data = comparatif.criteres
    .filter((c) => c.poids > 0)
    .map((c) => ({
      critere: describeStatus('critereEvaluation', c.code).label,
      ...Object.fromEntries(series.map((s) => [s.key, s.offre.notes[c.code] ?? 0])),
    }))
  return (
    <div className="grid gap-3">
      {series.length > 1 && <ChartLegend items={series} />}
      <ChartContainer label="Notes sur 100 de chaque offre pour chaque critère d'évaluation" height={300}>
        <BarChart data={data} barGap={2} margin={chartMargin}>
          <CartesianGrid {...gridProps} />
          <XAxis dataKey="critere" {...axisProps} interval={0} />
          <YAxis {...axisProps} domain={[0, 100]} width={yAxisWidth(['100'])} />
          <Tooltip
            cursor={cursorProps}
            content={
              <ChartTooltipContent valueFormatter={(v) => `${formatNumber(v, { decimals: 1 })}/100`} />
            }
          />
          {series.map((s) => (
            <Bar
              key={s.key}
              dataKey={s.key}
              name={s.label}
              fill={s.color}
              radius={[4, 4, 0, 0]}
              maxBarSize={18}
            />
          ))}
        </BarChart>
      </ChartContainer>
      {comparatif.offres.length > MAX_OFFRES_GRAPHIQUE && (
        <p className="text-xs text-muted-foreground">
          Le graphique présente les {MAX_OFFRES_GRAPHIQUE} premières offres du classement ; le tableau les
          liste toutes.
        </p>
      )}
    </div>
  )
}
