import { parseISO } from 'date-fns'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { formatDateTime, toIsoDate } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { DataTable, useTableState, type ColumnDef } from '@/shared/ui/data-table'
import { DateRangePicker } from '@/shared/ui/date-picker'
import { FilterBar } from '@/shared/ui/filter-bar'
import { PageHeader } from '@/shared/ui/page-header'
import { Select } from '@/shared/ui/select'
import { AuditDetailSheet } from '../components/AuditDetailSheet'
import { AuditStats } from '../components/AuditStats'
import { AuditUtilisateurFilter } from '../components/AuditUtilisateurFilter'
import { DebouncedInput } from '../components/DebouncedInput'
import { useAuditLogs, useAuditStatistiques } from '../hooks/useAudit'
import type { AuditLog } from '../types'

const TOUS = 'TOUS'
const FILTER_KEYS = ['q', 'service', 'entite', 'action', 'utilisateurId', 'du', 'au'] as const
const DEFAULT_SORT = [{ id: 'occurredAt', desc: true }]

const columns: ColumnDef<AuditLog>[] = [
  {
    accessorKey: 'occurredAt',
    header: 'Date',
    meta: { nowrap: true },
    enableSorting: true,
    cell: ({ row }) => (
      <span className="whitespace-nowrap tabular-nums">{formatDateTime(row.original.occurredAt)}</span>
    ),
  },
  {
    accessorKey: 'utilisateurNom',
    header: 'Utilisateur',
    meta: { nowrap: true },
    cell: ({ row }) => row.original.utilisateurNom ?? <span className="text-muted-foreground">Système</span>,
  },
  {
    accessorKey: 'action',
    header: 'Action',
    meta: { mobilePrimary: true, nowrap: true },
    cell: ({ row }) => <span className="font-mono text-xs">{row.original.action}</span>,
  },
  {
    accessorKey: 'entite',
    header: 'Entité',
    meta: { nowrap: true },
    cell: ({ row }) => (
      <span className="grid">
        <span>{row.original.entite}</span>
        <span className="max-w-40 truncate font-mono text-2xs text-muted-foreground">
          {row.original.entiteId}
        </span>
      </span>
    ),
  },
  {
    accessorKey: 'service',
    header: 'Service',
    meta: { hideOnMobile: true, nowrap: true, hideBelow: 'xl' },
    cell: ({ row }) => <Badge size="sm">{row.original.service}</Badge>,
  },
  {
    accessorKey: 'resume',
    header: 'Résumé',
    meta: { hideOnMobile: true, minWidth: 240 },
    cell: ({ row }) => (
      <span className="line-clamp-2 max-w-md text-muted-foreground">{row.original.resume ?? ''}</span>
    ),
  },
]

/** Journal d'audit : filtres (utilisateur, entite, action, service, periode, recherche), detail lateral. */
export function Component() {
  const [params, setParams] = useSearchParams()
  const table = useTableState({ defaultSort: DEFAULT_SORT, defaultPageSize: 50 })
  const [selected, setSelected] = useState<AuditLog | null>(null)
  const filters = Object.fromEntries(
    FILTER_KEYS.map((key) => [key, params.get(key) ?? undefined]),
  ) as Partial<Record<(typeof FILTER_KEYS)[number], string>>
  const query = useAuditLogs({ ...filters, ...table.pageParams })
  const services = useAuditStatistiques().data?.parService.map((item) => item.service) ?? []

  const setFilter = (key: (typeof FILTER_KEYS)[number], value: string | undefined) => {
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
  const hasFilters = FILTER_KEYS.some((key) => params.has(key))

  return (
    <>
      <PageHeader
        title="Journal d'audit"
      />
      <div className="grid gap-6">
        <AuditStats />
        <DataTable
          label="Journal d'audit"
          columns={columns}
          data={query.data}
          getRowId={(log) => log.id}
          isLoading={query.isPending}
          isFetching={query.isFetching}
          error={query.error}
          onRetry={() => void query.refetch()}
          pagination={table.pagination}
          onPaginationChange={table.onPaginationChange}
          sorting={table.sorting}
          onSortingChange={table.onSortingChange}
          onRowClick={setSelected}
          toolbar={
            <FilterBar
              search={filters.q ?? ''}
              onSearchChange={(value) => {
                setFilter('q', value || undefined)
              }}
              searchPlaceholder="Rechercher dans le journal…"
              canReset={hasFilters}
              onReset={() => {
                setParams(new URLSearchParams(), { replace: true })
              }}
            >
              <AuditUtilisateurFilter
                value={filters.utilisateurId}
                onChange={(value) => {
                  setFilter('utilisateurId', value)
                }}
              />
              <DebouncedInput
                aria-label="Entité"
                placeholder="Entité (ex. Besoin)"
                containerClassName="sm:w-40"
                className="sm:w-40"
                value={filters.entite ?? ''}
                onValueChange={(value) => {
                  setFilter('entite', value.trim() || undefined)
                }}
              />
              <DebouncedInput
                aria-label="Action"
                placeholder="Action (ex. BESOIN_VALIDE)"
                className="sm:w-48"
                value={filters.action ?? ''}
                onValueChange={(value) => {
                  setFilter('action', value.trim() || undefined)
                }}
              />
              <Select
                aria-label="Service"
                className="w-auto min-w-40"
                value={filters.service ?? TOUS}
                options={[
                  { value: TOUS, label: 'Tous les services' },
                  ...services.map((service) => ({ value: service, label: service })),
                ]}
                onValueChange={(value) => {
                  setFilter('service', value === TOUS ? undefined : value)
                }}
              />
              <DateRangePicker
                className="sm:w-64"
                placeholder="Période"
                max={new Date()}
                value={{
                  from: filters.du ? parseISO(filters.du) : null,
                  to: filters.au ? parseISO(filters.au) : null,
                }}
                onChange={(range) => {
                  setParams(
                    (current) => {
                      const next = new URLSearchParams(current)
                      const du = toIsoDate(range.from)
                      const au = toIsoDate(range.to)
                      if (du) next.set('du', du)
                      else next.delete('du')
                      if (au) next.set('au', au)
                      else next.delete('au')
                      next.delete('page')
                      return next
                    },
                    { replace: true },
                  )
                }}
              />
            </FilterBar>
          }
        />
      </div>
      <AuditDetailSheet
        log={selected}
        onClose={() => {
          setSelected(null)
        }}
      />
    </>
  )
}
