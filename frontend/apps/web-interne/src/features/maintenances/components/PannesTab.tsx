import { AlertOctagon, Wrench } from 'lucide-react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { useHasRole } from '@/shared/auth'
import { STATUT_PANNE, optionsOf } from '@/shared/domain/labels'
import { formatDateTime, formatRelative } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { DataTable, useTableState, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { FilterBar } from '@/shared/ui/filter-bar'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Panne } from '../api/types'
import { usePannes } from '../hooks/useMaintenances'
import { useEnumParam } from '../lib/use-enum-param'
import { EnumFilter } from './EnumFilter'

interface Props {
  onSignaler: () => void
  /** Ouvre la planification d'un OT correctif pre-rempli depuis la panne (RM). */
  onCreerOt: (panne: Panne) => void
}

/** Pannes signalees : gravite, statut, lien vers l'OT ou creation de l'OT (RM). */
export function PannesTab({ onSignaler, onCreerOt }: Props) {
  const { t } = useTranslation('maintenances')
  const gestion = useHasRole(access.maintenanceGestion)
  const peutSignaler = useHasRole(access.panneSignalement)
  const table = useTableState({ prefix: 'p_' })
  const [statut, setStatut] = useEnumParam('pstatut', STATUT_PANNE, 'p_page')
  const query = usePannes({ statut, page: table.pageParams.page, size: table.pageParams.size })

  const columns = useMemo<ColumnDef<Panne>[]>(
    () => [
      {
        accessorKey: 'description',
        header: t('columns.panne'),
        meta: { mobilePrimary: true, minWidth: 260 },
        cell: ({ row }) => (
          <div className="min-w-0 max-w-xl">
            <p className="line-clamp-1 font-medium" title={row.original.description}>
              {row.original.description}
            </p>
            <Link
              to={to.materielDetail(row.original.materielId)}
              className="block truncate text-xs text-muted-foreground hover:text-foreground hover:underline"
            >
              <span className="font-mono">{row.original.materielCode}</span> · {row.original.materielDesignation}
              {row.original.chantierNom ? ` · ${row.original.chantierNom}` : ''}
            </Link>
          </div>
        ),
      },
      {
        accessorKey: 'gravite',
        header: t('columns.gravite'),
        meta: { nowrap: true },
        cell: ({ row }) => <StatusBadge kind="gravitePanne" value={row.original.gravite} />,
      },
      {
        accessorKey: 'statut',
        header: t('columns.statut'),
        meta: { nowrap: true },
        cell: ({ row }) => <StatusBadge kind="statutPanne" value={row.original.statut} />,
      },
      {
        accessorKey: 'dateSignalement',
        header: t('columns.dateSignalement'),
        meta: { nowrap: true },
        cell: ({ row }) => (
          <span title={formatDateTime(row.original.dateSignalement)}>
            {formatRelative(row.original.dateSignalement)}
            <span className="block text-xs text-muted-foreground">{row.original.signaleParNom}</span>
          </span>
        ),
      },
      {
        id: 'ot',
        header: t('columns.actions'),
        enableHiding: false,
        meta: { align: 'right', label: t('columns.actions') },
        cell: ({ row }) =>
          row.original.maintenanceId ? (
            <span className="text-sm text-muted-foreground">{t('actions.otLie')}</span>
          ) : gestion && row.original.statut !== 'RESOLUE' ? (
            <Button
              size="sm"
              variant="secondary"
              leftIcon={<Wrench />}
              onClick={() => {
                onCreerOt(row.original)
              }}
            >
              {t('actions.creerOt')}
            </Button>
          ) : null,
      },
    ],
    [t, gestion, onCreerOt],
  )

  return (
    <DataTable
      label={t('pannes.tableLabel')}
      columns={columns}
      data={query.data}
      getRowId={(p) => p.id}
      isLoading={query.isPending}
      isFetching={query.isFetching}
      error={query.error}
      onRetry={() => void query.refetch()}
      pagination={table.pagination}
      onPaginationChange={table.onPaginationChange}
      toolbar={
        <FilterBar
          canReset={statut !== undefined}
          onReset={() => {
            setStatut(undefined)
          }}
        >
          <EnumFilter
            label={t('pannes.statut')}
            options={optionsOf('statutPanne')}
            value={statut}
            onChange={setStatut}
          />
        </FilterBar>
      }
      emptyState={
        statut !== undefined ? (
          <EmptyState title={t('pannes.emptyFilteredTitle')} />
        ) : (
          <EmptyState
            icon={<AlertOctagon aria-hidden />}
            title={t('pannes.emptyTitle')}
            action={peutSignaler ? <Button onClick={onSignaler}>{t('page.signaler')}</Button> : undefined}
          />
        )
      }
    />
  )
}
