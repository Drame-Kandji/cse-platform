import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CartesianGrid, Line, LineChart, Tooltip, XAxis, YAxis } from 'recharts'
import { access } from '@/app/access'
import { Can } from '@/shared/auth'
import { labelOf, type CategorieMateriel } from '@/shared/domain/labels'
import { formatDate, formatMoney } from '@/shared/lib/format'
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
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import type { LigneCatalogue } from '../api/types'
import { useCatalogue, useHistoriquePrix } from '../hooks/useFournisseursQueries'
import { LigneCatalogueDialog } from './LigneCatalogueDialog'

const compact = (value: number) => formatMoney(value, { compact: true })

function HistoriquePrix({ fournisseurId, ligne }: { fournisseurId: string; ligne?: LigneCatalogue }) {
  const { t } = useTranslation('fournisseurs')
  const query = useHistoriquePrix(fournisseurId, ligne?.reference)
  if (!ligne) return <EmptyState size="compact" title={t('catalogue.historiqueHint')} />
  if (query.isPending) return <Skeleton className="h-56 w-full" />
  if (query.isError)
    return <ErrorState error={query.error} size="compact" onRetry={() => void query.refetch()} />
  const serie = [...query.data]
    .sort((a, b) => a.date.localeCompare(b.date))
    .map((point) => ({ date: formatDate(point.date, 'dd/MM/yy'), prix: point.prixUnitaire }))
  if (serie.length === 0) return <EmptyState size="compact" title={t('catalogue.historiqueHint')} />
  return (
    <ChartContainer label={`${t('catalogue.historiqueLabel')} : ${ligne.designation}`} height={220}>
      <LineChart data={serie} margin={chartMargin}>
        <CartesianGrid {...gridProps} />
        <XAxis dataKey="date" {...axisProps} />
        <YAxis
          {...axisProps}
          width={yAxisWidthFor(
            serie.map((p) => p.prix),
            compact,
          )}
          tickFormatter={compact}
        />
        <Tooltip
          cursor={cursorProps}
          content={<ChartTooltipContent valueFormatter={(v) => formatMoney(v)} />}
        />
        <Line type="stepAfter" dataKey="prix" name="Prix unitaire" stroke={chartColor(0)} strokeWidth={2} />
      </LineChart>
    </ChartContainer>
  )
}

/** Onglet "Catalogue" : articles references et historique de prix de l'article choisi. */
export function CatalogueTab({
  fournisseurId,
  specialites,
}: {
  fournisseurId: string
  specialites: readonly CategorieMateriel[]
}) {
  const { t } = useTranslation('fournisseurs')
  const query = useCatalogue(fournisseurId)
  const [selected, setSelected] = useState<string>()
  const ligne = query.data?.find((item) => item.reference === selected) ?? query.data?.[0]

  const columns: ColumnDef<LigneCatalogue>[] = [
    {
      accessorKey: 'reference',
      header: 'Référence',
      enableSorting: true,
      meta: { cellClassName: 'font-mono', nowrap: true },
    },
    {
      accessorKey: 'designation',
      header: 'Désignation',
      enableSorting: true,
      meta: { mobilePrimary: true, minWidth: 220 },
    },
    {
      accessorKey: 'categorie',
      header: 'Catégorie',
      meta: { nowrap: true },
      cell: ({ row }) => labelOf('categorieMateriel', row.original.categorie),
    },
    {
      accessorKey: 'prixUnitaire',
      header: 'Prix unitaire',
      enableSorting: true,
      meta: { align: 'right' },
      cell: ({ row }) => `${formatMoney(row.original.prixUnitaire)} / ${row.original.unite}`,
    },
    {
      accessorKey: 'delaiLivraisonJours',
      header: 'Délai',
      meta: { align: 'right' },
      cell: ({ row }) => `${String(row.original.delaiLivraisonJours)} j`,
    },
    {
      accessorKey: 'dateMaj',
      header: 'Mise à jour',
      meta: { nowrap: true },
      cell: ({ row }) => formatDate(row.original.dateMaj),
    },
  ]

  return (
    <div className="grid gap-4">
      <DataTable
        label={t('catalogue.tableLabel')}
        columns={columns}
        data={query.data}
        isLoading={query.isPending}
        error={query.error}
        onRetry={() => void query.refetch()}
        getRowId={(row) => row.id}
        onRowClick={(row) => {
          setSelected(row.reference)
        }}
        enableColumnVisibility={false}
        emptyState={<EmptyState title={t('catalogue.empty')} description={t('catalogue.emptyDescription')} />}
        toolbar={
          <div className="flex items-center justify-between gap-2">
            <h3 className="text-sm font-semibold">{t('catalogue.title')}</h3>
            <Can roles={access.achatsGestion}>
              <LigneCatalogueDialog fournisseurId={fournisseurId} specialites={specialites} />
            </Can>
          </div>
        }
      />
      <Card>
        <CardHeader
          title={t('catalogue.historique')}
          description={ligne ? `${ligne.reference} · ${ligne.designation}` : undefined}
        />
        <CardContent>
          <HistoriquePrix fournisseurId={fournisseurId} ligne={ligne} />
        </CardContent>
      </Card>
    </div>
  )
}
