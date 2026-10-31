import { useTranslation } from 'react-i18next'
import { Bar, BarChart, CartesianGrid, Cell, Tooltip, XAxis, YAxis } from 'recharts'
import { formatMoney } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { ChartContainer, ChartTooltipContent } from '@/shared/ui/chart'
import { axisProps, chartColor, cursorProps, gridProps } from '@/shared/ui/chart-theme'
import { Skeleton } from '@/shared/ui/skeleton'
import type { SyntheseFinance } from '../api/types'

const compactMoney = (value: number) => formatMoney(value, { compact: true })

/** Comparaison engage / facture / mandate / paye (une barre par etape, libelles visibles). */
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
      <CardHeader title={t('chart.title')} description={t('chart.description')} />
      <CardContent>
        {loading || !synthese ? (
          <Skeleton className="h-[240px] w-full" />
        ) : (
          <ChartContainer label={t('chart.label')} height={240}>
            <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid {...gridProps} />
              <XAxis dataKey="etape" {...axisProps} />
              <YAxis {...axisProps} width={88} tickFormatter={compactMoney} />
              <Tooltip
                cursor={cursorProps}
                content={<ChartTooltipContent valueFormatter={(value) => formatMoney(value)} />}
              />
              <Bar dataKey="montant" name={t('columns.montant')} radius={[4, 4, 0, 0]} maxBarSize={56}>
                {data.map((entry, index) => (
                  <Cell key={entry.etape} fill={chartColor(index)} />
                ))}
              </Bar>
            </BarChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  )
}
