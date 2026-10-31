import { Plus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { useHasRole } from '@/shared/auth'
import { optionsOf, type StatutFacture } from '@/shared/domain/labels'
import { formatDate, formatMoney } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { DataTable, useTableState, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { FilterBar, FilterChip } from '@/shared/ui/filter-bar'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Switch } from '@/shared/ui/switch'
import type { Facture } from '../api/types'
import { useFactures } from '../hooks/useFinance'
import { FactureCreateDialog } from './FactureCreateDialog'
import { FactureRowActions } from './FactureRowActions'
import { MandaterDialog } from './MandaterDialog'
import { RejeterFactureDialog } from './RejeterFactureDialog'

/** Tri par defaut (liste blanche API : dateEmission, dateEcheance, numero, statut). */
const TRI_DEFAUT = [{ id: 'dateEmission', desc: true }]

/** Onglet des factures : filtres, retards mis en evidence, actions DAF. */
export function FacturesPanel() {
  const { t } = useTranslation('finance')
  const peutGerer = useHasRole(access.financeGestion)
  const peutSaisir = useHasRole(access.factureSaisie)
  const table = useTableState({ prefix: 'f', defaultSort: TRI_DEFAUT })
  const [statut, setStatut] = useState<StatutFacture | undefined>(undefined)
  const [enRetard, setEnRetard] = useState(false)
  const [aMandater, setAMandater] = useState<Facture | null>(null)
  const [aRejeter, setARejeter] = useState<Facture | null>(null)
  const [creation, setCreation] = useState(false)
  const query = useFactures({ statut, enRetard, ...table.pageParams })
  const resetPage = () => {
    table.onPaginationChange((p) => ({ ...p, pageIndex: 0 }))
  }

  const columns = useMemo<ColumnDef<Facture>[]>(() => {
    const base: ColumnDef<Facture>[] = [
      {
        accessorKey: 'numero',
        header: t('columns.numero'),
        enableSorting: true,
        meta: { mobilePrimary: true, nowrap: true },
        cell: ({ row }) => (
          <span className="grid">
            <span className="font-medium">{row.original.numero}</span>
            <span className="max-w-56 truncate text-xs text-muted-foreground">
              {row.original.fournisseurNom}
            </span>
          </span>
        ),
      },
      {
        id: 'contrat',
        header: t('columns.contrat'),
        meta: { nowrap: true },
        cell: ({ row }) => (
          <Link
            to={to.contratDetail(row.original.contratId)}
            className="text-primary-emphasis hover:underline"
          >
            {row.original.contratNumero}
          </Link>
        ),
      },
      {
        accessorKey: 'montantTtc',
        header: t('columns.montantTtc'),
        meta: { align: 'right', nowrap: true },
        cell: ({ row }) => (
          <span className="tabular-nums" title={`HT ${formatMoney(row.original.montantHt)}`}>
            {formatMoney(row.original.montantTtc)}
          </span>
        ),
      },
      {
        accessorKey: 'dateEcheance',
        header: t('columns.echeance'),
        meta: { nowrap: true },
        enableSorting: true,
        cell: ({ row }) => (
          <span className="grid">
            <span className="tabular-nums">{formatDate(row.original.dateEcheance)}</span>
            {row.original.enRetard && (
              <span className="text-xs font-medium text-danger-emphasis">{t('factures.overdue')}</span>
            )}
          </span>
        ),
      },
      {
        accessorKey: 'statut',
        header: t('columns.statut'),
        meta: { nowrap: true },
        enableSorting: true,
        cell: ({ row }) => {
          const secondaire = row.original.motifRejet ?? row.original.referenceMandat
          return (
            <span className="grid justify-items-start gap-0.5">
              <StatusBadge kind="statutFacture" value={row.original.statut} />
              {secondaire && (
                <span className="max-w-48 truncate text-xs text-muted-foreground" title={secondaire}>
                  {secondaire}
                </span>
              )}
            </span>
          )
        },
      },
    ]
    if (!peutGerer) return base
    return [
      ...base,
      {
        id: 'actions',
        header: t('columns.actions'),
        meta: { align: 'right', label: t('columns.actions'), nowrap: true },
        cell: ({ row }) => (
          <FactureRowActions facture={row.original} onMandater={setAMandater} onRejeter={setARejeter} />
        ),
      },
    ]
  }, [t, peutGerer])

  return (
    <>
      <DataTable
        label={t('factures.label')}
        columns={columns}
        data={query.data}
        getRowId={(row) => row.id}
        isLoading={query.isPending}
        isFetching={query.isFetching}
        error={query.error}
        onRetry={() => void query.refetch()}
        pagination={table.pagination}
        onPaginationChange={table.onPaginationChange}
        sorting={table.sorting}
        onSortingChange={table.onSortingChange}
        enableColumnVisibility={false}
        emptyState={<EmptyState title={t('factures.empty')} description={t('factures.emptyDescription')} />}
        toolbar={
          <FilterBar
            canReset={statut !== undefined || enRetard}
            onReset={() => {
              setStatut(undefined)
              setEnRetard(false)
              resetPage()
            }}
            actions={
              peutSaisir && (
                <Button
                  size="sm"
                  leftIcon={<Plus />}
                  onClick={() => {
                    setCreation(true)
                  }}
                >
                  {t('factures.create')}
                </Button>
              )
            }
          >
            <FilterChip
              label={t('factures.statut')}
              options={optionsOf('statutFacture')}
              value={statut ? [statut] : []}
              onChange={(value) => {
                setStatut(value.at(-1))
                resetPage()
              }}
            />
            <Switch
              label={t('factures.enRetard')}
              checked={enRetard}
              onCheckedChange={(checked) => {
                setEnRetard(checked)
                resetPage()
              }}
            />
          </FilterBar>
        }
      />
      <MandaterDialog
        facture={aMandater}
        onClose={() => {
          setAMandater(null)
        }}
      />
      <RejeterFactureDialog
        facture={aRejeter}
        onClose={() => {
          setARejeter(null)
        }}
      />
      <FactureCreateDialog open={creation} onOpenChange={setCreation} />
    </>
  )
}
