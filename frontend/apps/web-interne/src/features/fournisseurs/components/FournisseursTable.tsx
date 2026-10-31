import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { to } from '@/app/routes'
import {
  CATEGORIE_MATERIEL,
  labelOf,
  optionsOf,
  STATUT_FOURNISSEUR,
  type CategorieMateriel,
  type StatutFournisseur,
} from '@/shared/domain/labels'
import { formatNumber } from '@/shared/lib/format'
import { DataTable, useTableState, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { FilterBar } from '@/shared/ui/filter-bar'
import { Select } from '@/shared/ui/select'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Fournisseur } from '../api/types'
import { useFournisseurs } from '../hooks/useFournisseursQueries'
import { FournisseursStats } from './FournisseursStats'
import { ScoreBar } from './ScoreBar'

const TOUTES = '__toutes__'

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
          <span className="truncate text-xs text-muted-foreground">
            {row.original.specialites.map((s, i) => (
              <span key={s}>
                {i > 0 && ', '}
                <span>{labelOf('categorieMateriel', s)}</span>
              </span>
            ))}
          </span>
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
      accessorKey: 'statut',
      header: t('columns.statut'),
      meta: { nowrap: true },
      enableSorting: true,
      cell: ({ row }) => (
        <div className="grid justify-items-start gap-0.5">
          <StatusBadge kind="statutFournisseur" value={row.original.statut} />
          {row.original.documentsExpires > 0 && (
            <span className="text-xs text-danger-emphasis">
              {t('stats.documentsExpires', { count: row.original.documentsExpires })}
            </span>
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
      cell: ({ row }) =>
        row.original.nombreLitigesOuverts > 0 ? formatNumber(row.original.nombreLitigesOuverts) : '—',
    },
  ]

  const hasFilters = Boolean(q || statut || specialite)
  return (
    <>
      <FournisseursStats
        value={statut}
        onChange={(value) => {
          setFilter('statut', value)
        }}
      />
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
      emptyState={<EmptyState title={t('list.empty')} />}
      toolbar={
        <FilterBar
          search={q}
          onSearchChange={(value) => {
            setFilter('q', value || undefined)
          }}
          searchPlaceholder={t('list.search')}
          canReset={hasFilters}
          moreCount={specialite ? 1 : 0}
          moreFilters={
            <Select
              aria-label={t('filters.specialite')}
              options={[
                { value: TOUTES, label: `${t('filters.specialite')} : toutes` },
                ...optionsOf('categorieMateriel'),
              ]}
              value={specialite ?? TOUTES}
              onValueChange={(value) => {
                setFilter('specialite', value === TOUTES ? undefined : value)
              }}
            />
          }
          onReset={() => {
            setParams(new URLSearchParams(), { replace: true })
          }}
        >
        </FilterBar>
      }
    />
    </>
  )
}
