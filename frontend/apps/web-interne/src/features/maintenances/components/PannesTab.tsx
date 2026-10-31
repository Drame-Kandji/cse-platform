import { AlertOctagon, Link2, Wrench } from 'lucide-react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { useHasRole } from '@/shared/auth'
import { STATUT_PANNE, optionsOf } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatDateTime, formatRelative } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
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
        accessorKey: 'gravite',
        header: t('columns.gravite'),
        meta: { nowrap: true },
        cell: ({ row }) => <StatusBadge kind="gravitePanne" value={row.original.gravite} />,
      },
      {
        accessorKey: 'materielDesignation',
        header: t('columns.materiel'),
        meta: { mobilePrimary: true, minWidth: 200 },
        cell: ({ row }) => (
          <Link to={to.materielDetail(row.original.materielId)} className="group block min-w-0">
            <span className="line-clamp-2 font-medium group-hover:underline">
              {row.original.materielDesignation}
            </span>
            <span className="block font-mono text-xs text-muted-foreground">{row.original.materielCode}</span>
          </Link>
        ),
      },
      {
        accessorKey: 'description',
        header: t('columns.description'),
        meta: { minWidth: 240 },
        cell: ({ row }) => <p className="line-clamp-2 max-w-md text-sm">{row.original.description}</p>,
      },
      {
        accessorKey: 'chantierNom',
        header: t('columns.chantier'),
        meta: { hideOnMobile: true, minWidth: 160, hideBelow: 'xl' },
        cell: ({ row }) => row.original.chantierNom ?? EMPTY_VALUE,
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
            <Badge tone="primary">
              <Link2 aria-hidden />
              {t('actions.otLie')}
            </Badge>
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
            description={t('pannes.emptyDescription')}
            action={peutSignaler ? <Button onClick={onSignaler}>{t('page.signaler')}</Button> : undefined}
          />
        )
      }
    />
  )
}
