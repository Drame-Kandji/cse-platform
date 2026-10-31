import { AlertTriangle, FileSignature } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { to } from '@/app/routes'
import { optionsOf, type StatutContrat } from '@/shared/domain/labels'
import { formatDate, formatMoney } from '@/shared/lib/format'
import { Combobox, EmptyState, FilterBar, FilterChip, PageHeader, StatusBadge } from '@/shared/ui'
import { DataTable, useTableState, type ColumnDef } from '@/shared/ui/data-table'
import type { Contrat } from '../api/types'
import { livraisonEnRetard } from '../components/contrat-domain'
import { useContrats, useFournisseursOptions } from '../hooks/useContrats'

/** Liste des contrats et commandes. */
export function Component() {
  const { t } = useTranslation('contrats')
  const navigate = useNavigate()
  const table = useTableState({ defaultSort: [{ id: 'numero', desc: true }] })
  const [statut, setStatut] = useState<StatutContrat | null>(null)
  const [fournisseurId, setFournisseurId] = useState<string | null>(null)
  const [q, setQ] = useState('')
  const fournisseurs = useFournisseursOptions()
  const query = useContrats({
    statut: statut ?? undefined,
    fournisseurId: fournisseurId ?? undefined,
    q: q || undefined,
    ...table.pageParams,
  })

  const resetPage = () => {
    table.onPaginationChange((p) => ({ ...p, pageIndex: 0 }))
  }

  const columns = useMemo<ColumnDef<Contrat>[]>(
    () => [
      {
        accessorKey: 'numero',
        header: t('columns.numero'),
        enableSorting: true,
        enableHiding: false,
        cell: ({ row }) => <span className="font-mono text-xs font-medium">{row.original.numero}</span>,
      },
      {
        accessorKey: 'objet',
        header: t('columns.objet'),
        meta: { mobilePrimary: true },
        cell: ({ row }) => (
          <div className="grid min-w-0 max-w-xs">
            <span className="truncate font-medium">{row.original.objet}</span>
            <span className="truncate text-xs text-muted-foreground">{row.original.designation}</span>
          </div>
        ),
      },
      { accessorKey: 'fournisseurNom', header: t('columns.fournisseur') },
      {
        accessorKey: 'montant',
        header: t('columns.montant'),
        meta: { align: 'right' },
        cell: ({ row }) => formatMoney(row.original.montant),
      },
      {
        accessorKey: 'statut',
        header: t('columns.statut'),
        enableSorting: true,
        cell: ({ row }) => <StatusBadge kind="statutContrat" value={row.original.statut} />,
      },
      {
        accessorKey: 'dateLivraisonPrevue',
        header: t('columns.livraison'),
        enableSorting: true,
        meta: { hideOnMobile: true },
        cell: ({ row }) => (
          <span className="inline-flex items-center gap-1.5 tabular-nums">
            {formatDate(row.original.dateLivraisonPrevue)}
            {livraisonEnRetard(row.original) && (
              <span className="inline-flex items-center gap-1 text-xs font-medium text-danger-emphasis">
                <AlertTriangle className="size-3.5" aria-hidden />
                {t('detail.late')}
              </span>
            )}
          </span>
        ),
      },
    ],
    [t],
  )

  const fournisseurOptions = (fournisseurs.data?.content ?? []).map((f) => ({
    value: f.id,
    label: f.raisonSociale,
  }))
  const canReset = statut !== null || fournisseurId !== null || q !== ''

  return (
    <>
      <PageHeader title={t('list.title')} description={t('list.description')} />
      <DataTable
        label={t('list.tableLabel')}
        columns={columns}
        data={query.data}
        getRowId={(c) => c.id}
        isLoading={query.isPending}
        isFetching={query.isFetching}
        error={query.error}
        onRetry={() => void query.refetch()}
        pagination={table.pagination}
        onPaginationChange={table.onPaginationChange}
        sorting={table.sorting}
        onSortingChange={table.onSortingChange}
        onRowClick={(c) => {
          navigate(to.contratDetail(c.id))
        }}
        emptyState={
          canReset ? undefined : (
            <EmptyState
              icon={<FileSignature aria-hidden />}
              title={t('list.emptyTitle')}
              description={t('list.emptyDescription')}
            />
          )
        }
        toolbar={
          <FilterBar
            search={q}
            onSearchChange={(value) => {
              setQ(value)
              resetPage()
            }}
            searchPlaceholder={t('list.search')}
            canReset={canReset}
            onReset={() => {
              setStatut(null)
              setFournisseurId(null)
              setQ('')
              resetPage()
            }}
          >
            <FilterChip
              label={t('list.statut')}
              options={optionsOf('statutContrat')}
              value={statut ? [statut] : []}
              onChange={(value) => {
                setStatut(value.at(-1) ?? null)
                resetPage()
              }}
            />
            <Combobox
              className="sm:w-60"
              placeholder={t('list.fournisseur')}
              options={fournisseurOptions}
              loading={fournisseurs.isPending}
              value={fournisseurId}
              onValueChange={(value) => {
                setFournisseurId(value)
                resetPage()
              }}
            />
          </FilterBar>
        }
      />
    </>
  )
}
