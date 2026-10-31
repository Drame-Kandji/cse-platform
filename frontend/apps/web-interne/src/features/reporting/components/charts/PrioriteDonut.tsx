import { Cell, Pie, PieChart, Tooltip } from 'recharts'
import { labelOf, PRIORITE, type Priorite } from '@/shared/domain/labels'
import { formatNumber, formatPercent } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { ChartContainer, ChartTooltipContent } from '@/shared/ui/chart'
import { chartColor } from '@/shared/ui/chart-theme'
import { EmptyState } from '@/shared/ui/empty-state'
import type { SectionBesoins } from '../../types'

/** Couleurs ordinales alignees sur les badges de priorite (normale / urgente / critique). */
const COLORS: Readonly<Record<Priorite, string>> = {
  NORMAL: chartColor(0),
  URGENT: 'rgb(var(--warning))',
  CRITIQUE: 'rgb(var(--danger))',
}

/** Besoins de la periode par priorite (anneau + legende chiffree). */
export function PrioriteDonut({ besoins }: { besoins: SectionBesoins }) {
  const data = PRIORITE.map((priorite) => ({
    priorite,
    name: labelOf('priorite', priorite),
    value: besoins.parPriorite[priorite] ?? 0,
  }))
  const total = data.reduce((sum, item) => sum + item.value, 0)
  return (
    <Card className="flex h-full flex-col">
      <CardHeader
        title="Besoins par priorité"
        description={`${formatNumber(total)} besoins sur la période`}
      />
      <CardContent className="flex flex-1 flex-col items-center gap-4 sm:flex-row lg:flex-col xl:flex-row">
        {total === 0 ? (
          <EmptyState
            size="compact"
            title="Aucun besoin"
            description="Aucun besoin exprimé sur la période."
          />
        ) : (
          <>
            <div className="relative w-44 shrink-0">
              <ChartContainer height={176} label="Répartition des besoins par priorité">
                <PieChart>
                  <Tooltip
                    content={<ChartTooltipContent valueFormatter={(value) => formatNumber(value)} />}
                  />
                  <Pie
                    data={data}
                    dataKey="value"
                    nameKey="name"
                    innerRadius="68%"
                    outerRadius="100%"
                    paddingAngle={2}
                    stroke="rgb(var(--surface))"
                    strokeWidth={2}
                    isAnimationActive={false}
                  >
                    {data.map((item) => (
                      <Cell key={item.priorite} fill={COLORS[item.priorite]} />
                    ))}
                  </Pie>
                </PieChart>
              </ChartContainer>
              <div className="pointer-events-none absolute inset-0 grid place-content-center text-center">
                <span className="text-2xl font-semibold tabular-nums">{formatNumber(total)}</span>
                <span className="text-2xs text-muted-foreground">besoins</span>
              </div>
            </div>
            <ul className="grid w-full gap-2.5 text-sm">
              {data.map((item) => (
                <li key={item.priorite} className="flex items-center gap-2.5">
                  <span
                    className="size-2.5 shrink-0 rounded-sm"
                    style={{ backgroundColor: COLORS[item.priorite] }}
                    aria-hidden
                  />
                  <span className="flex-1 text-muted-foreground">{item.name}</span>
                  <span className="font-medium tabular-nums">{formatNumber(item.value)}</span>
                  <span className="w-12 text-right text-xs tabular-nums text-muted-foreground">
                    {formatPercent(item.value / total, { decimals: 0 })}
                  </span>
                </li>
              ))}
            </ul>
          </>
        )}
      </CardContent>
    </Card>
  )
}
