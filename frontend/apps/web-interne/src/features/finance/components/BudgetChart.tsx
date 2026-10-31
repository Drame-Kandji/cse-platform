import { useTranslation } from 'react-i18next'
import { Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from 'recharts'
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
import { Skeleton } from '@/shared/ui/skeleton'
import type { SyntheseFinance } from '../api/types'

const compactMoney = (value: number) => formatMoney(value, { compact: true })

/** Engage / facture / mandate / paye cumules (une seule serie bleue). */
export function BudgetChart({
  synthese,
  loading,
}: {
  synthese: SyntheseFinance | undefined
  loading: boolean
}) {
  const { t } = useTranslation('finance')
  const data = synthese
    ? [
        { etape: t('chart.engage'), montant: synthese.montantEngage },
        { etape: t('chart.facture'), montant: synthese.montantFacture },
        { etape: t('chart.mandate'), montant: synthese.montantMandate },
        { etape: t('chart.paye'), montant: synthese.montantPaye },
      ]
    : []
  return (
    <Card>
      <CardHeader title={t('chart.title')} />
      <CardContent>
        {loading || !synthese ? (
          <Skeleton className="h-[220px] w-full" />
        ) : (
          <ChartContainer label={t('chart.label')} height={220}>
            <BarChart data={data} margin={chartMargin}>
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
              <Bar
                dataKey="montant"
                name={t('columns.montant')}
                fill={chartColor(0)}
                radius={[4, 4, 0, 0]}
                maxBarSize={48}
              />
            </BarChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  )
}
