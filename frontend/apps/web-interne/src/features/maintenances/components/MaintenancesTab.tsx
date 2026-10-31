import { CalendarClock, CheckSquare, Play, Wrench } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { useHasRole } from '@/shared/auth'
import { STATUT_MAINTENANCE, TYPE_MAINTENANCE, optionsOf } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatDate, formatMoney } from '@/shared/lib/format'
import { ConfirmDialog } from '@/shared/ui/alert-dialog'
import { Button } from '@/shared/ui/button'
import { DataTable, useTableState, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { FilterBar } from '@/shared/ui/filter-bar'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Maintenance } from '../api/types'
import { useDemarrerMaintenance, useMaintenances } from '../hooks/useMaintenances'
import { useEnumParam } from '../lib/use-enum-param'
import { CloturerDialog } from './CloturerDialog'
import { EnumFilter } from './EnumFilter'

const DEFAULT_SORT = [{ id: 'datePlanifiee', desc: true }]

function DemarrerAction({ maintenance }: { maintenance: Maintenance }) {
  const { t } = useTranslation('maintenances')
  const demarrer = useDemarrerMaintenance()
  return (
    <ConfirmDialog
      title={t('demarrer.title', { numero: maintenance.numeroOt })}
      description={t('demarrer.description', { materiel: maintenance.materielCode })}
      confirmLabel={t('demarrer.confirmer')}
      onConfirm={() =>
        new Promise<void>((resolve) => {
          demarrer.mutate(maintenance.id, {
            onSettled: () => {
              resolve()
            },
          })
        })
      }
      trigger={
        <Button size="sm" variant="secondary" leftIcon={<Play />}>
          {t('actions.demarrer')}
        </Button>
      }
    />
  )
}

/** Ordres de travail : filtres statut / type, demarrage et cloture (RM). */
export function MaintenancesTab({ onPlanifier }: { onPlanifier: () => void }) {
  const { t } = useTranslation('maintenances')
  const gestion = useHasRole(access.maintenanceGestion)
  const table = useTableState({ prefix: 'm_', defaultSort: DEFAULT_SORT })
  const [statut, setStatut] = useEnumParam('mstatut', STATUT_MAINTENANCE, 'm_page')
  const [type, setType] = useEnumParam('mtype', TYPE_MAINTENANCE, 'm_page')
  const [aCloturer, setACloturer] = useState<Maintenance | null>(null)
  const query = useMaintenances({ statut, type, ...table.pageParams })
  const filtres = Boolean(statut ?? type)

  const columns = useMemo<ColumnDef<Maintenance>[]>(
    () => [
      {
        accessorKey: 'numeroOt',
        header: t('columns.numeroOt'),
        enableSorting: true,
        enableHiding: false,
        cell: ({ row }) => <span className="font-mono text-xs font-medium">{row.original.numeroOt}</span>,
      },
      {
        accessorKey: 'materielDesignation',
        header: t('columns.materiel'),
        meta: { mobilePrimary: true },
        cell: ({ row }) => (
          <Link to={to.materielDetail(row.original.materielId)} className="group block min-w-0">
            <span className="block truncate font-medium group-hover:underline">
              {row.original.materielDesignation}
            </span>
            <span className="block truncate text-xs text-muted-foreground">
              {row.original.materielCode}
              {row.original.chantierNom ? ` · ${row.original.chantierNom}` : ''}
            </span>
          </Link>
        ),
      },
      {
        accessorKey: 'type',
        header: t('columns.type'),
        cell: ({ row }) => <StatusBadge kind="typeMaintenance" value={row.original.type} dot={false} />,
      },
      {
        accessorKey: 'statut',
        header: t('columns.statut'),
        cell: ({ row }) => <StatusBadge kind="statutMaintenance" value={row.original.statut} />,
      },
      {
        accessorKey: 'datePlanifiee',
        header: t('columns.datePlanifiee'),
        enableSorting: true,
        cell: ({ row }) => <span className="tabular-nums">{formatDate(row.original.datePlanifiee)}</span>,
      },
      {
        accessorKey: 'technicien',
        header: t('columns.technicien'),
        meta: { hideOnMobile: true },
        cell: ({ row }) => row.original.technicien ?? EMPTY_VALUE,
      },
      {
        accessorKey: 'cout',
        header: t('columns.cout'),
        meta: { align: 'right' },
        cell: ({ row }) => formatMoney(row.original.cout),
      },
      ...(gestion
        ? [
            {
              id: 'actions',
              header: t('columns.actions'),
              enableHiding: false,
              meta: { align: 'right' as const, label: t('columns.actions') },
              cell: ({ row }: { row: { original: Maintenance } }) =>
                row.original.statut === 'PLANIFIEE' ? (
                  <DemarrerAction maintenance={row.original} />
                ) : row.original.statut === 'EN_COURS' ? (
                  <Button
                    size="sm"
                    leftIcon={<CheckSquare />}
                    onClick={() => {
                      setACloturer(row.original)
                    }}
                  >
                    {t('actions.cloturer')}
                  </Button>
                ) : null,
            } satisfies ColumnDef<Maintenance>,
          ]
        : []),
    ],
    [t, gestion],
  )

  return (
    <>
      <DataTable
        label={t('maintenances.tableLabel')}
        columns={columns}
        data={query.data}
        getRowId={(m) => m.id}
        isLoading={query.isPending}
        isFetching={query.isFetching}
        error={query.error}
        onRetry={() => void query.refetch()}
        pagination={table.pagination}
        onPaginationChange={table.onPaginationChange}
        sorting={table.sorting}
        onSortingChange={table.onSortingChange}
        toolbar={
          <FilterBar
            canReset={filtres}
            onReset={() => {
              setStatut(undefined)
              setType(undefined)
            }}
          >
            <EnumFilter
              label={t('maintenances.statut')}
              options={optionsOf('statutMaintenance')}
              value={statut}
              onChange={setStatut}
            />
            <EnumFilter
              label={t('maintenances.type')}
              options={optionsOf('typeMaintenance')}
              value={type}
              onChange={setType}
            />
          </FilterBar>
        }
        emptyState={
          filtres ? (
            <EmptyState icon={<CalendarClock aria-hidden />} title={t('maintenances.emptyFilteredTitle')} />
          ) : (
            <EmptyState
              icon={<Wrench aria-hidden />}
              title={t('maintenances.emptyTitle')}
              description={t('maintenances.emptyDescription')}
              action={gestion ? <Button onClick={onPlanifier}>{t('page.planifier')}</Button> : undefined}
            />
          )
        }
      />
      <CloturerDialog
        maintenance={aCloturer}
        onClose={() => {
          setACloturer(null)
        }}
      />
    </>
  )
}
