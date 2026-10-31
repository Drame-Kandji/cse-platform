import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { to } from '@/app/routes'
import { optionsOf, type StatutAO, type TypeAppelOffre } from '@/shared/domain/labels'
import { formatDateTime, formatMoney, formatNumber } from '@/shared/lib/format'
import { DataTable, useTableState, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { FilterBar, FilterChip } from '@/shared/ui/filter-bar'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { AppelOffre } from '../api/types'
import { useAppelsOffres } from '../hooks/useAppelsOffres'
import { Countdown } from './Countdown'

const columns: ColumnDef<AppelOffre>[] = [
  {
    accessorKey: 'reference',
    header: 'Référence',
    meta: { nowrap: true },
    enableSorting: true,
    enableHiding: false,
    cell: ({ row }) => <span className="font-mono text-xs">{row.original.reference}</span>,
  },
  {
    accessorKey: 'objet',
    header: 'Objet',
    meta: { mobilePrimary: true, minWidth: 240 },
    cell: ({ row }) => (
      <div className="min-w-0">
        <p className="line-clamp-2 font-medium">{row.original.objet}</p>
        {row.original.chantierNom && (
          <p className="line-clamp-1 text-xs text-muted-foreground">{row.original.chantierNom}</p>
        )}
      </div>
    ),
  },
  {
    accessorKey: 'type',
    header: 'Type',
    meta: { hideOnMobile: true, nowrap: true, hideBelow: 'xl' },
    cell: ({ row }) => <StatusBadge kind="typeAppelOffre" value={row.original.type} dot={false} />,
  },
  {
    accessorKey: 'statut',
    header: 'Statut',
    meta: { nowrap: true },
    cell: ({ row }) => <StatusBadge kind="statutAO" value={row.original.statut} />,
  },
  {
    accessorKey: 'dateLimite',
    header: 'Date limite',
    meta: { nowrap: true },
    enableSorting: true,
    cell: ({ row }) => (
      <div className="grid">
        <span className="tabular-nums">{formatDateTime(row.original.dateLimite)}</span>
        {row.original.statut === 'PUBLIE' && <Countdown dateLimite={row.original.dateLimite} />}
      </div>
    ),
  },
  {
    accessorKey: 'nombreOffres',
    header: 'Offres',
    meta: { align: 'right' },
    cell: ({ row }) => formatNumber(row.original.nombreOffres),
  },
  {
    id: 'montant',
    header: 'Budget / attribué',
    meta: { align: 'right', label: 'Budget ou montant attribué' },
    cell: ({ row }) => formatMoney(row.original.montantAttribue ?? row.original.budgetEstime),
  },
]

/** Liste filtrable des appels d'offres (statut, type, recherche). */
export function AppelsOffresTable() {
  const navigate = useNavigate()
  const table = useTableState({ defaultSort: [{ id: 'dateLimite', desc: true }] })
  const [statut, setStatut] = useState<StatutAO>()
  const [type, setType] = useState<TypeAppelOffre>()
  const [q, setQ] = useState('')
  const query = useAppelsOffres({ statut, type, q: q || undefined, ...table.pageParams })
  const resetPage = () => {
    table.onPaginationChange((p) => ({ ...p, pageIndex: 0 }))
  }
  const filtresActifs = statut !== undefined || type !== undefined || q !== ''

  return (
    <DataTable
      label="Appels d'offres"
      columns={columns}
      data={query.data}
      getRowId={(ao) => ao.id}
      isLoading={query.isPending}
      isFetching={query.isFetching}
      error={query.error}
      onRetry={() => void query.refetch()}
      pagination={table.pagination}
      onPaginationChange={table.onPaginationChange}
      sorting={table.sorting}
      onSortingChange={table.onSortingChange}
      onRowClick={(ao) => {
        navigate(to.appelOffreDetail(ao.id))
      }}
      emptyState={
        <EmptyState
          title={filtresActifs ? 'Aucun appel d’offres ne correspond' : 'Aucun appel d’offres'}
          description={
            filtresActifs
              ? 'Modifiez les filtres ou la recherche.'
              : 'Lancez un appel d’offres à partir d’un besoin à acheter.'
          }
        />
      }
      toolbar={
        <FilterBar
          search={q}
          onSearchChange={(value) => {
            setQ(value)
            resetPage()
          }}
          searchPlaceholder="Référence, objet…"
          canReset={filtresActifs}
          onReset={() => {
            setStatut(undefined)
            setType(undefined)
            setQ('')
            resetPage()
          }}
        >
          <FilterChip
            label="Statut"
            options={optionsOf('statutAO')}
            value={statut ? [statut] : []}
            onChange={(value) => {
              setStatut(value.at(-1))
              resetPage()
            }}
          />
          <FilterChip
            label="Type"
            options={optionsOf('typeAppelOffre')}
            value={type ? [type] : []}
            onChange={(value) => {
              setType(value.at(-1))
              resetPage()
            }}
          />
        </FilterBar>
      }
    />
  )
}
