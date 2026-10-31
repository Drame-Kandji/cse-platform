import { Area, AreaChart, Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from 'recharts'
import { formatMoney, formatPercent } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { ChartContainer, ChartLegend, ChartTooltipContent } from '@/shared/ui/chart'
import { axisProps, chartColor, cursorProps, gridProps } from '@/shared/ui/chart-theme'
import { MONTHLY } from './demo-data'
import { MapDemo } from './MapDemo'
import { Section } from './Section'

const compactMoney = (value: number) => formatMoney(value, { compact: true })

/** Graphiques Recharts relies aux jetons + carte Leaflet. */
export function ChartsSection() {
  return (
    <Section
      id="graphiques"
      title="Graphiques & carte"
      description="Recharts avec les jetons chart-1..6, infobulle et légende du design system ; react-leaflet."
    >
      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title="Disponibilité du parc" description="Taux mensuel, 2026" />
          <CardContent>
            <ChartContainer label="Taux de disponibilité mensuel du parc en 2026">
              <AreaChart data={MONTHLY} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="dispo" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0%" stopColor={chartColor(0)} stopOpacity={0.2} />
                    <stop offset="100%" stopColor={chartColor(0)} stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid {...gridProps} />
                <XAxis dataKey="mois" {...axisProps} />
                <YAxis
                  {...axisProps}
                  width={52}
                  domain={[0.6, 1]}
                  tickFormatter={(v: number) => formatPercent(v, { decimals: 0 })}
                />
                <Tooltip
                  cursor={cursorProps}
                  content={<ChartTooltipContent valueFormatter={(v) => formatPercent(v)} />}
                />
                <Area
                  type="monotone"
                  dataKey="disponibilite"
                  name="Disponibilité"
                  stroke={chartColor(0)}
                  strokeWidth={2}
                  fill="url(#dispo)"
                  activeDot={{ r: 4, strokeWidth: 2, stroke: 'rgb(var(--surface))' }}
                />
              </AreaChart>
            </ChartContainer>
          </CardContent>
        </Card>
        <Card>
          <CardHeader
            title="Dépenses mensuelles"
            description="Maintenance et achats, FCFA"
            action={
              <ChartLegend
                items={[
                  { label: 'Maintenance', color: chartColor(0) },
                  { label: 'Achats', color: chartColor(1) },
                ]}
              />
            }
          />
          <CardContent>
            <ChartContainer label="Dépenses mensuelles de maintenance et d'achats en francs CFA">
              <BarChart data={MONTHLY} barGap={2} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid {...gridProps} />
                <XAxis dataKey="mois" {...axisProps} />
                <YAxis {...axisProps} width={88} tickFormatter={compactMoney} />
                <Tooltip
                  cursor={cursorProps}
                  content={<ChartTooltipContent valueFormatter={(v) => formatMoney(v)} />}
                />
                <Bar
                  dataKey="maintenance"
                  name="Maintenance"
                  fill={chartColor(0)}
                  radius={[4, 4, 0, 0]}
                  maxBarSize={18}
                />
                <Bar
                  dataKey="achats"
                  name="Achats"
                  fill={chartColor(1)}
                  radius={[4, 4, 0, 0]}
                  maxBarSize={18}
                />
              </BarChart>
            </ChartContainer>
          </CardContent>
        </Card>
      </div>
      <MapDemo />
    </Section>
  )
}
