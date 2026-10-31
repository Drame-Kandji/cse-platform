import { Check, X } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { to } from '@/app/routes'
import { useHasRole } from '@/shared/auth'
import { access } from '@/app/access'
import { optionsOf, type StatutApprobation } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatDate, formatMoney } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { DataTable, useTableState, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { Select } from '@/shared/ui/select'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Engagement } from '../api/types'
import { useEngagements } from '../hooks/useFinance'
import { EngagementDecisionDialog } from './EngagementDecisionDialog'

type FiltreStatut = StatutApprobation | 'TOUS'

/** Tri par defaut (liste blanche API : dateDemande, dateDecision). */
const TRI_DEFAUT = [{ id: 'dateDemande', desc: true }]

/** Onglet des engagements budgetaires (validation DAF, lecture RA/DG). */
export function EngagementsPanel() {
  const { t } = useTranslation('finance')
  const peutDecider = useHasRole(access.financeGestion)
  const table = useTableState({ prefix: 'e', defaultSort: TRI_DEFAUT })
  const [statut, setStatut] = useState<FiltreStatut>('EN_ATTENTE')
  const [decision, setDecision] = useState<{ engagement: Engagement; approuve: boolean } | null>(null)
  const query = useEngagements({ statut: statut === 'TOUS' ? undefined : statut, ...table.pageParams })

  const columns = useMemo<ColumnDef<Engagement>[]>(() => {
    const base: ColumnDef<Engagement>[] = [
      {
        id: 'contratNumero',
        header: t('columns.contrat'),
        meta: { mobilePrimary: true, nowrap: true },
        cell: ({ row }) => (
          <Link
            to={to.contratDetail(row.original.contratId)}
            className="font-medium text-primary-emphasis hover:underline"
          >
            {row.original.contratNumero}
          </Link>
        ),
      },
      {
        accessorKey: 'fournisseurNom',
        header: t('columns.fournisseur'),
        meta: { minWidth: 160 },
      },
      {
        accessorKey: 'objet',
        header: t('columns.objet'),
        meta: { minWidth: 200 },
        cell: ({ row }) => (
          <span className="grid">
            <span className="line-clamp-2">{row.original.objet}</span>
            {row.original.chantierNom && (
              <span className="text-xs text-muted-foreground">{row.original.chantierNom}</span>
            )}
          </span>
        ),
      },
      {
        accessorKey: 'montant',
        header: t('columns.montant'),
        meta: { align: 'right' },
        cell: ({ row }) => formatMoney(row.original.montant),
      },
      {
        accessorKey: 'dateDemande',
        header: t('columns.dateDemande'),
        meta: { nowrap: true },
        enableSorting: true,
        cell: ({ row }) => formatDate(row.original.dateDemande),
      },
      {
        accessorKey: 'statut',
        header: t('columns.statut'),
        meta: { nowrap: true },
        cell: ({ row }) => <StatusBadge kind="statutApprobation" value={row.original.statut} />,
      },
      {
        id: 'decision',
        header: t('columns.decision'),
        meta: { hideBelow: '2xl' },
        cell: ({ row }) =>
          row.original.dateDecision
            ? `${row.original.parNom ?? EMPTY_VALUE}, ${formatDate(row.original.dateDecision)}`
            : EMPTY_VALUE,
      },
    ]
    if (!peutDecider) return base
    return [
      ...base,
      {
        id: 'actions',
        header: t('columns.actions'),
        meta: { align: 'right', label: t('columns.actions'), nowrap: true },
        cell: ({ row }) =>
          row.original.statut === 'EN_ATTENTE' ? (
            <div className="flex justify-end gap-2">
              <Button
                size="sm"
                variant="secondary"
                leftIcon={<X />}
                onClick={() => {
                  setDecision({ engagement: row.original, approuve: false })
                }}
              >
                {t('engagements.refuse')}
              </Button>
              <Button
                size="sm"
                leftIcon={<Check />}
                onClick={() => {
                  setDecision({ engagement: row.original, approuve: true })
                }}
              >
                {t('engagements.approve')}
              </Button>
            </div>
          ) : null,
      },
    ]
  }, [t, peutDecider])

  return (
    <>
      <DataTable
        label={t('engagements.label')}
        columns={columns}
        data={query.data}
        getRowId={(row) => row.contratId}
        isLoading={query.isPending}
        isFetching={query.isFetching}
        error={query.error}
        onRetry={() => void query.refetch()}
        pagination={table.pagination}
        onPaginationChange={table.onPaginationChange}
        sorting={table.sorting}
        onSortingChange={table.onSortingChange}
        enableColumnVisibility={false}
        emptyState={
          <EmptyState title={t('engagements.empty')} description={t('engagements.emptyDescription')} />
        }
        toolbar={
          <div className="w-full sm:w-56">
            <Select<FiltreStatut>
              aria-label={t('engagements.statut')}
              options={[{ value: 'TOUS', label: t('engagements.all') }, ...optionsOf('statutApprobation')]}
              value={statut}
              onValueChange={(value) => {
                setStatut(value)
                table.onPaginationChange((p) => ({ ...p, pageIndex: 0 }))
              }}
            />
          </div>
        }
      />
      <EngagementDecisionDialog
        engagement={decision?.engagement ?? null}
        approuve={decision?.approuve ?? true}
        onClose={() => {
          setDecision(null)
        }}
      />
    </>
  )
}
