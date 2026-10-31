import { AlarmClock, Plus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { useHasRole } from '@/shared/auth'
import { optionsOf, type StatutFacture } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { EMPTY_VALUE, formatDate, formatMoney } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
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
        meta: { mobilePrimary: true },
        cell: ({ row }) => (
          <span className="flex flex-wrap items-center gap-2 font-medium">
            {row.original.numero}
            {row.original.enRetard && (
              <Badge tone="danger" size="sm">
                <AlarmClock aria-hidden />
                {t('factures.overdue')}
              </Badge>
            )}
          </span>
        ),
      },
      {
        id: 'contrat',
        header: t('columns.contrat'),
        cell: ({ row }) => (
          <Link
            to={to.contratDetail(row.original.contratId)}
            className="text-primary-emphasis hover:underline"
          >
            {row.original.contratNumero}
          </Link>
        ),
      },
      { accessorKey: 'fournisseurNom', header: t('columns.fournisseur') },
      {
        accessorKey: 'montantTtc',
        header: t('columns.montantTtc'),
        meta: { align: 'right' },
        cell: ({ row }) => (
          <span className="grid">
            <span className="font-medium">{formatMoney(row.original.montantTtc)}</span>
            <span className="text-xs text-muted-foreground">HT {formatMoney(row.original.montantHt)}</span>
          </span>
        ),
      },
      {
        accessorKey: 'dateEcheance',
        header: t('columns.echeance'),
        enableSorting: true,
        cell: ({ row }) => (
          <span className={cn(row.original.enRetard && 'font-medium text-danger-emphasis')}>
            {formatDate(row.original.dateEcheance)}
          </span>
        ),
      },
      {
        accessorKey: 'statut',
        header: t('columns.statut'),
        enableSorting: true,
        cell: ({ row }) => (
          <span className="grid gap-0.5">
            <StatusBadge kind="statutFacture" value={row.original.statut} />
            {row.original.motifRejet && (
              <span
                className="max-w-48 truncate text-xs text-muted-foreground"
                title={row.original.motifRejet}
              >
                {row.original.motifRejet}
              </span>
            )}
          </span>
        ),
      },
      {
        accessorKey: 'referenceMandat',
        header: t('columns.mandat'),
        cell: ({ row }) => row.original.referenceMandat ?? EMPTY_VALUE,
      },
    ]
    if (!peutGerer) return base
    return [
      ...base,
      {
        id: 'actions',
        header: t('columns.actions'),
        meta: { align: 'right', label: t('columns.actions') },
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
