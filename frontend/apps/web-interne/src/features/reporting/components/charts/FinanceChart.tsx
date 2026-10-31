import { Bar, BarChart, CartesianGrid, Cell, LabelList, Tooltip, XAxis, YAxis } from 'recharts'
import { formatMoney } from '@/shared/lib/format'
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
import { compactMoney, formatTaux } from '../../lib/format'
import type { SectionFinances } from '../../types'

/** Chaine budgetaire : budget, engage, mandate, paye (une seule serie, budget en reference grisee). */
export function FinanceChart({ finances }: { finances: SectionFinances }) {
  const data = [
    { etape: 'Budget', montant: finances.budgetTotal, reference: true },
    { etape: 'Engagé', montant: finances.montantEngage, reference: false },
    { etape: 'Mandaté', montant: finances.montantMandate, reference: false },
    { etape: 'Payé', montant: finances.montantPaye, reference: false },
  ]
  return (
    <Card className="flex h-full flex-col">
      <CardHeader
        title="Exécution budgétaire"
        description={`${formatTaux(finances.tauxExecutionBudget)} du budget engagé · montants en FCFA`}
      />
      <CardContent className="flex-1">
        <ChartContainer height={260} label="Budget total, montants engagé, mandaté et payé en francs CFA">
          <BarChart data={data} margin={{ ...chartMargin, top: 20 }} barCategoryGap="30%">
            <CartesianGrid {...gridProps} />
            <XAxis dataKey="etape" {...axisProps} />
            <YAxis
              {...axisProps}
              width={yAxisWidthFor(
                data.map((d) => d.montant),
                compactMoney,
              )}
              tickFormatter={compactMoney}
            />
            <Tooltip
              cursor={cursorProps}
              content={<ChartTooltipContent valueFormatter={(value) => formatMoney(value)} />}
            />
            <Bar dataKey="montant" name="Montant" radius={[4, 4, 0, 0]} maxBarSize={56}>
              {data.map((item) => (
                <Cell key={item.etape} fill={item.reference ? 'rgb(var(--border-strong))' : chartColor(0)} />
              ))}
              <LabelList
                dataKey="montant"
                position="top"
                formatter={(value: number) => compactMoney(value)}
                className="fill-muted-foreground text-2xs"
              />
            </Bar>
          </BarChart>
        </ChartContainer>
      </CardContent>
    </Card>
  )
}
