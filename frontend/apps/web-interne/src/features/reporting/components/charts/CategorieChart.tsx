import { Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from 'recharts'
import { labelOf } from '@/shared/domain/labels'
import { formatNumber } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { ChartContainer, ChartLegend, ChartTooltipContent } from '@/shared/ui/chart'
import {
  axisProps,
  chartColor,
  chartMargin,
  cursorProps,
  gridProps,
  yAxisWidthFor,
} from '@/shared/ui/chart-theme'
import type { ParcCategorie } from '../../types'

/** Couleurs alignees sur les badges d'etat matériel (disponible / affecte / maintenance). */
const SERIES = [
  { key: 'disponibles', label: 'Disponibles', color: chartColor(1) },
  { key: 'affectes', label: 'Affectés', color: chartColor(0) },
  { key: 'enMaintenance', label: 'En maintenance', color: chartColor(2) },
] as const

/** Repartition du parc par categorie (barres empilees par etat). */
export function CategorieChart({ categories }: { categories: readonly ParcCategorie[] }) {
  const data = categories.map((categorie) => ({
    ...categorie,
    libelle: labelOf('categorieMateriel', categorie.categorie),
  }))
  return (
    <Card className="flex h-full flex-col">
      <CardHeader
        title="Parc par catégorie"
        description="Nombre de matériels par état"
        action={<ChartLegend items={SERIES.map(({ label, color }) => ({ label, color }))} />}
      />
      <CardContent className="flex-1">
        <ChartContainer
          height={260}
          label="Nombre de matériels disponibles, affectés et en maintenance par catégorie"
        >
          <BarChart data={data} margin={chartMargin} barCategoryGap="28%">
            <CartesianGrid {...gridProps} />
            <XAxis
              dataKey="libelle"
              {...axisProps}
              interval={0}
              tickFormatter={(value: string) => value.split(' ')[0] ?? value}
            />
            <YAxis
              {...axisProps}
              width={yAxisWidthFor(
                data.map((d) => d.total),
                String,
              )}
              allowDecimals={false}
            />
            <Tooltip
              cursor={cursorProps}
              content={<ChartTooltipContent valueFormatter={(value) => formatNumber(value)} />}
            />
            {SERIES.map((serie, index) => (
              <Bar
                key={serie.key}
                dataKey={serie.key}
                name={serie.label}
                stackId="parc"
                fill={serie.color}
                maxBarSize={44}
                radius={index === SERIES.length - 1 ? [4, 4, 0, 0] : [0, 0, 0, 0]}
              />
            ))}
          </BarChart>
        </ChartContainer>
      </CardContent>
    </Card>
  )
}
