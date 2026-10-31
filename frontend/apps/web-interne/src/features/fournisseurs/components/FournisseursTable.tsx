import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { to } from '@/app/routes'
import {
  CATEGORIE_MATERIEL,
  optionsOf,
  STATUT_FOURNISSEUR,
  type CategorieMateriel,
  type StatutFournisseur,
} from '@/shared/domain/labels'
import { formatNumber } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { DataTable, useTableState, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { FilterBar, FilterChip } from '@/shared/ui/filter-bar'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Fournisseur } from '../api/types'
import { useFournisseurs } from '../hooks/useFournisseursQueries'
import { ScoreBar } from './ScoreBar'
import { SpecialitesChips } from './SpecialitesChips'

function pick<V extends string>(allowed: readonly V[], value: string | null): V | undefined {
  return allowed.find((item) => item === value)
}

/** Onglet "Liste" : tableau pagine des fournisseurs avec filtres synchronises dans l'URL. */
export function FournisseursTable() {
  const { t } = useTranslation('fournisseurs')
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const table = useTableState({ defaultSort: [{ id: 'raisonSociale', desc: false }] })
  const q = params.get('q') ?? ''
  const statut = pick<StatutFournisseur>(STATUT_FOURNISSEUR, params.get('statut'))
  const specialite = pick<CategorieMateriel>(CATEGORIE_MATERIEL, params.get('specialite'))
  const query = useFournisseurs({ q: q || undefined, statut, specialite, ...table.pageParams })

  const setFilter = (key: string, value: string | undefined) => {
    setParams(
      (current) => {
        const next = new URLSearchParams(current)
        if (value) next.set(key, value)
        else next.delete(key)
        next.delete('page')
        return next
      },
      { replace: true },
    )
  }

  const columns: ColumnDef<Fournisseur>[] = [
    {
      accessorKey: 'raisonSociale',
      header: t('columns.raisonSociale'),
      enableSorting: true,
      enableHiding: false,
      meta: { mobilePrimary: true, minWidth: 220 },
      cell: ({ row }) => (
        <div className="grid min-w-0">
          <span className="truncate font-medium text-foreground">{row.original.raisonSociale}</span>
          <span className="text-xs text-muted-foreground">NINEA {row.original.ninea}</span>
        </div>
      ),
    },
    {
      accessorKey: 'ville',
      header: t('columns.ville'),
      meta: { nowrap: true },
      enableSorting: true,
      cell: ({ row }) => row.original.ville ?? '—',
    },
    {
      id: 'specialites',
      header: t('columns.specialites'),
      meta: { minWidth: 180, hideBelow: 'xl' },
      cell: ({ row }) => <SpecialitesChips specialites={row.original.specialites} />,
    },
    {
      accessorKey: 'statut',
      header: t('columns.statut'),
      meta: { nowrap: true },
      enableSorting: true,
      cell: ({ row }) => (
        <div className="flex flex-wrap items-center gap-1">
          <StatusBadge kind="statutFournisseur" value={row.original.statut} />
          {row.original.documentsExpires > 0 && (
            <Badge tone="danger" size="sm">
              {t('stats.documentsExpires', { count: row.original.documentsExpires })}
            </Badge>
          )}
        </div>
      ),
    },
    {
      accessorKey: 'scorePerformance',
      header: t('columns.score'),
      meta: { nowrap: true },
      enableSorting: true,
      cell: ({ row }) => <ScoreBar score={row.original.scorePerformance} />,
    },
    {
      accessorKey: 'nombreLitigesOuverts',
      header: t('columns.litiges'),
      meta: { align: 'right' },
      cell: ({ row }) => formatNumber(row.original.nombreLitigesOuverts),
    },
  ]

  const hasFilters = Boolean(q || statut || specialite)
  return (
    <DataTable
      label={t('list.tableLabel')}
      columns={columns}
      data={query.data}
      isLoading={query.isPending}
      isFetching={query.isFetching}
      error={query.error}
      onRetry={() => void query.refetch()}
      pagination={table.pagination}
      onPaginationChange={table.onPaginationChange}
      sorting={table.sorting}
      onSortingChange={table.onSortingChange}
      getRowId={(row) => row.id}
      onRowClick={(row) => {
        navigate(to.fournisseurDetail(row.id))
      }}
      emptyState={<EmptyState title={t('list.empty')} description={t('list.emptyDescription')} />}
      toolbar={
        <FilterBar
          search={q}
          onSearchChange={(value) => {
            setFilter('q', value || undefined)
          }}
          searchPlaceholder={t('list.search')}
          canReset={hasFilters}
          onReset={() => {
            setParams(new URLSearchParams(), { replace: true })
          }}
        >
          <FilterChip
            label={t('filters.statut')}
            options={optionsOf('statutFournisseur')}
            value={statut ? [statut] : []}
            onChange={(values) => {
              setFilter('statut', values.at(-1))
            }}
          />
          <FilterChip
            label={t('filters.specialite')}
            options={optionsOf('categorieMateriel')}
            value={specialite ? [specialite] : []}
            onChange={(values) => {
              setFilter('specialite', values.at(-1))
            }}
          />
        </FilterBar>
      }
    />
  )
}
