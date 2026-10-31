import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { to } from '@/app/routes'
import { optionsOf, type CategorieMateriel } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatNumber, formatPercent } from '@/shared/lib/format'
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { FilterBar, FilterChip } from '@/shared/ui/filter-bar'
import type { Classement } from '../api/types'
import { useClassement } from '../hooks/useFournisseursQueries'
import { ScoreBar } from './ScoreBar'
import { SpecialitesChips } from './SpecialitesChips'

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
        <span
          className={cn(
            'inline-grid size-7 place-items-center rounded-full text-xs font-semibold tabular-nums',
            row.original.rang <= 3 ? 'bg-primary/10 text-primary-emphasis' : 'bg-muted text-muted-foreground',
          )}
        >
          {row.original.rang}
        </span>
      ),
    },
    {
      accessorKey: 'raisonSociale',
      header: t('columns.raisonSociale'),
      meta: { mobilePrimary: true, minWidth: 200 },
      cell: ({ row }) => <span className="font-medium">{row.original.raisonSociale}</span>,
    },
    {
      id: 'specialites',
      header: t('columns.specialites'),
      meta: { minWidth: 180 },
      cell: ({ row }) => <SpecialitesChips specialites={row.original.specialites} />,
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
        return taux === null || taux === undefined ? '—' : formatPercent(taux / 100)
      },
    },
    {
      accessorKey: 'nombreEvaluations',
      header: t('columns.evaluations'),
      meta: { align: 'right' },
      cell: ({ row }) => formatNumber(row.original.nombreEvaluations),
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
      <p className="text-sm text-muted-foreground">{t('classement.description')}</p>
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
          <EmptyState title={t('classement.empty')} description={t('classement.emptyDescription')} />
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
