import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { to } from '@/app/routes'
import { labelOf, optionsOf, type CategorieMateriel } from '@/shared/domain/labels'
import { formatNumber, formatPercent } from '@/shared/lib/format'
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { FilterBar, FilterChip } from '@/shared/ui/filter-bar'
import type { Classement } from '../api/types'
import { useClassement } from '../hooks/useFournisseursQueries'
import { ScoreBar } from './ScoreBar'

/** Onglet "Classement" : fournisseurs actifs classes par score, filtrables par categorie. */
export function ClassementTab() {
  const { t } = useTranslation('fournisseurs')
  const navigate = useNavigate()
  const [categorie, setCategorie] = useState<CategorieMateriel | undefined>()
  const query = useClassement(categorie)

  const columns: ColumnDef<Classement>[] = [
    {
      accessorKey: 'rang',
      header: t('columns.rang'),
      meta: { align: 'center' },
      cell: ({ row }) => (
        <span className="tabular-nums text-muted-foreground">{row.original.rang}</span>
      ),
    },
    {
      accessorKey: 'raisonSociale',
      header: t('columns.raisonSociale'),
      meta: { mobilePrimary: true, minWidth: 200 },
      cell: ({ row }) => (
        <div className="grid min-w-0">
          <span className="truncate font-medium">{row.original.raisonSociale}</span>
          <span className="truncate text-xs text-muted-foreground">
            {row.original.specialites.map((s) => labelOf('categorieMateriel', s)).join(', ')}
          </span>
        </div>
      ),
    },
    {
      accessorKey: 'scorePerformance',
      header: t('columns.score'),
      meta: { nowrap: true },
      cell: ({ row }) => <ScoreBar score={row.original.scorePerformance} />,
    },
    {
      accessorKey: 'tauxLivraisonALHeure',
      header: t('columns.ponctualite'),
      meta: { align: 'right' },
      cell: ({ row }) => {
        const taux = row.original.tauxLivraisonALHeure
        return taux === null || taux === undefined ? '-' : formatPercent(taux / 100)
      },
    },
    {
      accessorKey: 'nombreLitiges',
      header: t('columns.litiges'),
      meta: { align: 'right' },
      cell: ({ row }) => formatNumber(row.original.nombreLitiges),
    },
  ]

  return (
    <div className="grid gap-3">
      <DataTable
        label={t('classement.tableLabel')}
        columns={columns}
        data={query.data}
        isLoading={query.isPending}
        isFetching={query.isFetching}
        error={query.error}
        onRetry={() => void query.refetch()}
        getRowId={(row) => row.fournisseurId}
        onRowClick={(row) => {
          navigate(to.fournisseurDetail(row.fournisseurId))
        }}
        enableColumnVisibility={false}
        emptyState={
          <EmptyState title={t('classement.empty')} />
        }
        toolbar={
          <FilterBar
            canReset={categorie !== undefined}
            onReset={() => {
              setCategorie(undefined)
            }}
          >
            <FilterChip
              label={t('filters.categorie')}
              options={optionsOf('categorieMateriel')}
              value={categorie ? [categorie] : []}
              onChange={(values) => {
                setCategorie(values.at(-1))
              }}
            />
          </FilterBar>
        }
      />
    </div>
  )
}
